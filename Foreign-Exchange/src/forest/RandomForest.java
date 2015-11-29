package forest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.gson.Gson;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import dtree.DecisionTree;

/**
 * This class represents a random forest. It contains methods to train a random
 * forest, use the random forest to make decisions, and test the performance of
 * the random forest.
 * 
 * @author zzuo
 *
 */
public class RandomForest implements Serializable {

	private static final long serialVersionUID = 6140645275698012409L;

	/*
	 * List of trees in the forest
	 */
	private List<DecisionTree> trees;

	/*
	 * Number of trees to grow
	 */
	private int N;

	/*
	 * Number of features used for building a tree
	 */
	private int numOfFeaturesToBuildTree;

	/*
	 * Number of training records used for building a tree
	 */
	private int numOfTrainRecordsToBuildTree;

	/*
	 * List of names of features
	 */
	private List<String> features;

	/*
	 * Fraction of training records to be used for training a tree
	 */
	private static final double FRACTION_TRAINING_RECORDS = 2.0 / 3.0;

	public RandomForest(int N) {
		this.N = N;
		this.trees = new ArrayList<>();
		this.features = new ArrayList<>();
	}

	/**
	 * Train the random forest
	 * 
	 * @param trainFileName
	 */
	public void train(Cluster cluster) {
		List<boolean[]> records = readInRecordsFromCassandra(cluster, true);

		createPerformanceTable(cluster, true);
		
		features = Arrays.asList(new String[] { "avg_bid", "range_bid",
				"diff_bid", "delta_bid", "spread" });

		numOfFeaturesToBuildTree = (int) Math.sqrt(features.size());
		
		numOfTrainRecordsToBuildTree = (int) (FRACTION_TRAINING_RECORDS
				* records.size());

		// grow N trees
		for (int i = 0; i < N; i++) {
			DecisionTree tree = new DecisionTree(features);
			trees.add(tree);

			Set<Integer> featureIds = selectFeatures();
			
			List<boolean[]> trainRecords = new ArrayList<>();
			List<boolean[]> testRecords = new ArrayList<>();

			splitTrainAndTestRecords(records, trainRecords, testRecords);

			tree.train(trainRecords, featureIds);

			// after adding a new tree, use the data not used in training down
			// the forest to get the performance statistics (error rate)
			double errRate = testInternal(testRecords);

			System.out.println((i + 1) + " trees, error rate: " + errRate
					+ ", accuracy: " + (1 - errRate));

			insertPerformance(cluster, (i + 1), 1 - errRate, true);
		}
	}

	/**
	 * Test the performance of this random forest. First load records from data
	 * file, and then test on these records
	 * 
	 * @param testFileName
	 */
	public void test(Cluster cluster) {
		List<boolean[]> records = readInRecordsFromCassandra(cluster, false);

		createPerformanceTable(cluster, false);

		double errRate = testInternal(records);

		insertPerformance(cluster, N, 1 - errRate, false);

		System.out.println(
				"Test error rate: " + errRate + ", accuracy: " + (1 - errRate));
	}

	/**
	 * Test the performance of this random forest, based on list of records
	 * 
	 * @param records
	 */
	private double testInternal(List<boolean[]> records) {
		int errCnt = 0;
		
		int trueToTrue = 0;
		int trueToFalse = 0;
		int falseToTrue = 0;
		int falseToFalse = 0;

		for (boolean[] r : records) {
			boolean decision = decide(r);

			// check if decision is the same with the label
			if (decision != r[r.length - 1]) {
				errCnt++;
			}
			
			if (decision == true && r[r.length - 1] == true) {
				trueToTrue ++;
			} else if (decision == false && r[r.length - 1] == true) {
				trueToFalse ++;
			} else if (decision == true && r[r.length - 1] == false) {
				falseToTrue ++;
			} else {
				falseToFalse ++;
			}
		}
		
		System.out.println(trueToTrue + " " + trueToFalse + " " + falseToTrue + " " + falseToFalse);

		double errRate = (double) errCnt / records.size();

		return errRate;
	}

	/**
	 * Make decision on a specific record. Return the majority vote of trees in
	 * the forest
	 * 
	 * @param record
	 *            record to decide on
	 * @return decision result
	 */
	public boolean decide(boolean[] record) {
		int pos = 0;
		int neg = 0;

		// let each tree to vote (decide), and count positive and negative votes
		for (int i = 0; i < trees.size(); i++) {
			boolean des = trees.get(i).decide(record);
			if (des) {
				pos++;
			} else {
				neg++;
			}
		}

		return pos >= neg;
	}

	/**
	 * Randomly select part of features from feature set to build a tree
	 * 
	 * @return indexes of selected features
	 */
	private Set<Integer> selectFeatures() {
		Set<Integer> featureIds = new HashSet<>();

		int n = features.size();
		
		for (int i = 0; i < numOfFeaturesToBuildTree; i++) {
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
		for (int i = 0; i < numOfTrainRecordsToBuildTree; i++) {
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

	/**
	 * Create validation (during training) or testing performance table
	 * 
	 * @param cluster
	 *            Cassandra cluster
	 * @param isTrain
	 *            is this for training
	 */
	private void createPerformanceTable(Cluster cluster, boolean isTrain) {
		Session session = cluster.connect("test");

		String tableName = isTrain ? "validation_perf" : "test_perf";

		session.execute("DROP TABLE IF EXISTS " + tableName);
		session.execute("CREATE TABLE " + tableName
				+ " (trees int PRIMARY KEY, accuracy double)");
	}

	/**
	 * Insert performance stats into Cassandra database table, for validation
	 * (during training) or testing
	 * 
	 * @param cluster
	 *            Cassandra cluster
	 * @param trees
	 *            current number of trees in forest
	 * @param accuracy
	 *            validation accuracy
	 * @param isTrain
	 *            is this for training
	 */
	private void insertPerformance(Cluster cluster, int trees, double accuracy,
			boolean isTrain) {
		Session session = cluster.connect("test");

		String tableName = isTrain ? "validation_perf" : "test_perf";

		StringBuilder sb = new StringBuilder();

		sb.append("INSERT INTO ");
		sb.append(tableName);
		sb.append(" (trees, accuracy) VALUES (");
		sb.append(trees).append(", ").append(accuracy).append(")");

		session.execute(sb.toString());
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
}
