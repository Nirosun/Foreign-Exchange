package forest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	public void train(String trainFileName) {
		List<boolean[]> records = readInRecords(trainFileName, true);

		numOfFeaturesToBuildTree = (int) Math.sqrt(features.size());

		numOfTrainRecordsToBuildTree = (int) (FRACTION_TRAINING_RECORDS * records
				.size());

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
		}
	}

	/**
	 * Test the performance of this random forest. First load records from data
	 * file, and then test on these records
	 * 
	 * @param testFileName
	 */
	public void test(String testFileName) {
		List<boolean[]> records = readInRecords(testFileName, false);

		double errRate = testInternal(records);

		System.out.println("Test error rate: " + errRate + ", accuracy: "
				+ (1 - errRate));
	}

	/**
	 * Test the performance of this random forest, based on list of records
	 * 
	 * @param records
	 */
	private double testInternal(List<boolean[]> records) {
		int errCnt = 0;

		for (boolean[] r : records) {
			boolean decision = decide(r);

			// check if decision is the same with the label
			if (decision != r[r.length - 1]) {
				errCnt++;
			}
		}

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
	 * Serialize forest into a file
	 * 
	 * @param forest
	 * @param fileName
	 */
	public static void serialize(RandomForest forest, String fileName) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(fileName));
			out.writeObject(forest);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deserialize a RandomForest object from a file
	 * 
	 * @param fileName
	 * @return
	 */
	public static RandomForest deserialize(String fileName) {
		RandomForest forest = null;

		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					fileName));
			forest = (RandomForest) in.readObject();
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		return forest;
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
	 * Read in records from data file (train or test)
	 * 
	 * @param fileName
	 * @param isTrain
	 *            is this for training (or for testing)
	 * @return
	 */
	private List<boolean[]> readInRecords(String fileName, boolean isTrain) {
		List<boolean[]> records = new ArrayList<>(); // records in boolean
														// format

		// read in records from training file
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			// if is for training, load feature names from first line of file
			if (isTrain) {
				features.addAll(Arrays.asList(line.split(",")));
				features.remove(features.size() - 1);
			}

			while ((line = br.readLine()) != null) {
				String[] strs = line.split(",");
				int len = strs.length;
				boolean[] binaries = new boolean[len];
				for (int i = 0; i < len; i++) {
					binaries[i] = Boolean.parseBoolean(strs[i]);
				}
				records.add(binaries);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return records;
	}
}
