package mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import dtree.DecisionTree;
import util.GlobalParams;
import util.SerializeUtil;

/**
 * The mapper class, read records from cassandra and train a decision tree using
 * randomly selected features and data
 * 
 * @author zzuo
 *
 */
public class RandomForestMapper extends Mapper<Object, Text, Text, Text> {
	/*
	 * Dummy key for generating result
	 */
	private static final Text DUMMY_KEY = new Text("dummy_key");

	/*
	 * List of names of features
	 */
	private static final List<String> FEATURES = Arrays.asList(new String[] {
			"avg_bid", "range_bid", "diff_bid", "delta_bid", "spread" });

	/*
	 * Number of features used for building a tree
	 */
	private static final int FEATURES_TO_BUILD_TREE = 2;

	/*
	 * Fraction of training records to be used for training a tree
	 */
	private static final double PROPORTION_OF_TRAIN_RECORDS = 2.0 / 3.0;

	@Override
	public void map(Object key, Text value, Context context)
			throws IOException, InterruptedException {

		DecisionTree tree = new DecisionTree(FEATURES);

		Set<Integer> featureIds = selectFeatures();

		List<boolean[]> trainRecords = new ArrayList<>();
		List<boolean[]> testRecords = new ArrayList<>();

		Cluster cluster = Cluster.builder()
				.addContactPoint(GlobalParams.CASSANDRA_ADDR).build();

		// get records
		List<boolean[]> records = readInRecordsFromCassandra(cluster, true);

		splitTrainAndTestRecords(records, trainRecords, testRecords);

		// train the tree
		tree.train(trainRecords, featureIds);

		// save tree as json
		String json = SerializeUtil.decisionTreeToJson(tree);

		// write json text
		context.write(DUMMY_KEY, new Text(json));
	}

	/**
	 * Randomly select part of features from feature set to build a tree
	 * 
	 * @return indexes of selected features
	 */
	private Set<Integer> selectFeatures() {
		Set<Integer> featureIds = new HashSet<>();

		int n = FEATURES.size();

		for (int i = 0; i < FEATURES_TO_BUILD_TREE; i++) {
			int id;
			// avoid duplicate IDs
			do {
				id = (int) (Math.random() * n);
			} while (featureIds.contains(id));
			featureIds.add(id);
		}

		return featureIds;
	}

	/**
	 * Read in data records from Cassandra database, can read data for training
	 * or testing
	 * 
	 * @param cluster
	 *            Cassandra cluster
	 * @param isTrain
	 *            is this for training
	 * @return list of boolean records
	 */
	private List<boolean[]> readInRecordsFromCassandra(Cluster cluster,
			boolean isTrain) {
		Session session = cluster.connect("test");

		ResultSet rs;

		if (isTrain) {
			rs = session.execute("SELECT * FROM train_data");
		} else {
			rs = session.execute("SELECT * FROM test_data");
		}

		List<boolean[]> results = new ArrayList<>();

		for (Row row : rs) {
			boolean[] binaries = new boolean[6];
			binaries[0] = row.getBool("avg_bid");
			binaries[1] = row.getBool("range_bid");
			binaries[2] = row.getBool("diff_bid");
			binaries[3] = row.getBool("delta_bid");
			binaries[4] = row.getBool("spread");
			binaries[5] = row.getBool("label");

			results.add(binaries);
		}

		return results;
	}

	/**
	 * Randomly split records for training and testing a tree from all records
	 * 
	 * @param allRecords
	 * @param trainRecords
	 * @param testRecords
	 */
	private void splitTrainAndTestRecords(List<boolean[]> allRecords,
			List<boolean[]> trainRecords, List<boolean[]> testRecords) {
		Set<Integer> trainIds = new HashSet<>();

		// randomly select enough records for training
		for (int i = 0; i < allRecords.size()
				* PROPORTION_OF_TRAIN_RECORDS; i++) {
			int id = (int) (Math.random() * allRecords.size());
			trainRecords.add(allRecords.get(id));
			trainIds.add(id);
		}

		// use unseen records for testing
		for (int i = 0; i < allRecords.size(); i++) {
			if (!trainIds.contains(i)) {
				testRecords.add(allRecords.get(i));
			}
		}
	}
}
