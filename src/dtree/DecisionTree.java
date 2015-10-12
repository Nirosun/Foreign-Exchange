package dtree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a decision tree. It includes methods to train the tree,
 * use the tree to make decision, and test performance of the tree
 * 
 * @author zzuo
 *
 */
public class DecisionTree implements Serializable {

	private static final long serialVersionUID = 3285710322176522954L;

	/*
	 * The root node of the decision tree
	 */
	private TreeNode root;

	/*
	 * names of all features
	 */
	private List<String> features;

	public DecisionTree(List<String> features) {
		this.root = new TreeNode(null);
		this.features = features;
	}

	/**
	 * Train the tree (used by RandomForest)
	 * 
	 * @param records
	 * @param featuresIds
	 *            feature indexes for this tree
	 */
	public void train(List<boolean[]> records, Set<Integer> featuresIds) {
		Set<Integer> recordIds = new HashSet<>(); // record IDs related to
													// current node

		for (int i = 0; i < records.size(); i++) {
			boolean[] binaries = records.get(i);
			int len = binaries.length;

			if (binaries[len - 1]) {
				root.pos++;
			} else {
				root.neg++;
			}

			recordIds.add(i);
		}

		trainNode(root, records, recordIds, featuresIds);
	}

	/**
	 * Train the tree (used when creating the tree in standalone mode, i.e. no
	 * random forest)
	 * 
	 * @param trainFileName
	 */
	public void train(String trainFileName) {
		// training records in boolean format
		List<boolean[]> records = new ArrayList<>();

		// record IDs related to current node
		Set<Integer> recordIds = new HashSet<>();

		// unused features until now
		Set<Integer> unusedFeatures = new HashSet<>();

		// initialize the root node and the collections above
		try (BufferedReader br = new BufferedReader(new FileReader(
				trainFileName))) {
			String line = br.readLine();
			features.addAll(Arrays.asList(line.split(",")));
			features.remove(features.size() - 1);

			for (int i = 0; i < features.size(); i++) {
				unusedFeatures.add(i);
			}

			int id = 0;
			while ((line = br.readLine()) != null) {
				String[] strs = line.split(",");
				int len = strs.length;
				boolean[] binaries = new boolean[len];
				for (int i = 0; i < len; i++) {
					binaries[i] = Boolean.parseBoolean(strs[i]);
				}

				if (binaries[len - 1]) {
					root.pos++;
				} else {
					root.neg++;
				}

				records.add(binaries);
				recordIds.add(id);

				id++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		trainNode(root, records, recordIds, unusedFeatures);
	}

	/**
	 * Test using trained decision tree
	 * 
	 * @param testFileName
	 * @return error rate
	 */
	public double test(String testFileName) {
		int errCnt = 0;
		int totalCnt = 0;

		try (BufferedReader br = new BufferedReader(
				new FileReader(testFileName))) {
			String line = br.readLine();

			while ((line = br.readLine()) != null) {
				String[] strs = line.split(",");
				int len = strs.length;
				boolean[] binaries = new boolean[len];

				for (int i = 0; i < len; i++) {
					binaries[i] = Boolean.parseBoolean(strs[i]);
				}

				// make decision on the record
				boolean decision = decide(binaries);

				if (decision != binaries[len - 1]) {
					errCnt++;
				}
				totalCnt++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return errCnt / (double) totalCnt;
	}

	/**
	 * Make a decision based on trained tree on a given record (API)
	 * 
	 * @param record
	 *            record to make decision on
	 * @return decision
	 */
	public boolean decide(boolean[] record) {
		return decide(root, record);
	}

	/**
	 * Make decision based on trained tree on a given record (for internal)
	 * 
	 * @param n
	 *            current node
	 * @param record
	 *            record to make decision on
	 * @return decision
	 */
	private boolean decide(TreeNode n, boolean[] record) {
		// if feature name is null, means this node is a leaf node
		// decision should be made on this node
		if (n.featureName == null) {
			return n.pos > n.neg;
		}

		for (int i = 0; i < features.size(); i++) {
			if (n.featureName.equals(features.get(i))) {
				if (record[i]) {
					return decide(n.left, record);
				} else {
					return decide(n.right, record);
				}
			}
		}

		assert (false) : "Shouldn't reach here.";
		return false;
	}

	/**
	 * Train a node
	 * 
	 * @param n
	 *            current node
	 * @param records
	 *            all training records
	 * @param recordIds
	 *            IDs of records related to this node
	 * @param unusedFeatures
	 *            features that haven't been used until current node (under
	 *            specific path)
	 */
	private void trainNode(TreeNode n, List<boolean[]> records,
			Set<Integer> recordIds, Set<Integer> unusedFeatures) {
		// check if there's no need to split on this node
		if (n.pos == 0 || n.neg == 0 || unusedFeatures.isEmpty()) {
			return;
		}

		double maxMI = 0.0; // maximum mutual information
		int maxId = -1; // id of the feature that maximize MI
		n.left = new TreeNode(null);
		n.right = new TreeNode(null);

		// consider all unused features for this node to split on
		for (int i : unusedFeatures) {
			n.featureName = features.get(i);

			n.left.pos = 0;
			n.left.neg = 0;
			n.right.pos = 0;
			n.right.neg = 0;

			// divide records to left and right subtrees
			for (int id : recordIds) {
				boolean[] record = records.get(id);
				int len = record.length;

				// assign records with positive feature values to left subtree
				if (record[i]) {
					if (record[len - 1]) {
						n.left.pos++;
					} else {
						n.left.neg++;
					}
				} else {
					if (record[len - 1]) {
						n.right.pos++;
					} else {
						n.right.neg++;
					}
				}
			}

			double mi = n.mutualInformation();
			if (mi > maxMI) {
				maxMI = mi;
				maxId = i;
			}
		}

		if (maxMI <= 0) {
			// no information gain, stop splitting
			n.featureName = null;
			n.left = null;
			n.right = null;
			return;
		}

		// split on the feature with max MI, and set up childs
		Set<Integer> leftRecordIds = new HashSet<>();
		Set<Integer> rightRecordIds = new HashSet<>();

		n.featureName = features.get(maxId);
		n.left.pos = 0;
		n.left.neg = 0;
		n.right.pos = 0;
		n.right.neg = 0;

		for (int id : recordIds) {
			boolean[] record = records.get(id);
			int len = record.length;
			if (record[maxId]) {
				leftRecordIds.add(id);
				if (record[len - 1]) {
					n.left.pos++;
				} else {
					n.left.neg++;
				}
			} else {
				rightRecordIds.add(id);
				if (record[len - 1]) {
					n.right.pos++;
				} else {
					n.right.neg++;
				}
			}
		}

		unusedFeatures.remove(maxId); // used current feature

		// train children nodes
		trainNode(n.left, records, leftRecordIds, new HashSet<>(unusedFeatures));
		trainNode(n.right, records, rightRecordIds, new HashSet<>(
				unusedFeatures));
	}
}
