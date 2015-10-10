package dtree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a decision tree
 * 
 * @author zzuo
 *
 */
public class DecisionTree {

	private Node root;

	private List<String> features; // names of features

	public DecisionTree(List<String> features) {
		this.root = new Node(null);
		this.features = features;
	}
	
	public void train(List<boolean[]> records, Set<Integer> unusedFeatures) {
		Set<Integer> recordIds = new HashSet<>(); // record IDs related to current node
		
		for (int i = 0; i < features.size(); i++) {
			unusedFeatures.add(i);
		}
		
		for (int i = 0; i < records.size(); i ++) {
			boolean[] binaries = records.get(i);
			int len = binaries.length;

			if (binaries[len - 1]) {
				root.pos++;
			} else {
				root.neg++;
			}

			recordIds.add(i);
		}
		
		trainNode(root, records, recordIds, unusedFeatures);		
	}

	/**
	 * Train the tree
	 * 
	 * @param trainFileName
	 */
	public void train(String trainFileName) {
		List<boolean[]> records = new ArrayList<>(); // training records in
														// boolean format
		Set<Integer> recordIds = new HashSet<>(); // record IDs related to
													// current node
		Set<Integer> unusedFeatures = new HashSet<>(); // unused features until
														// now

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
	 * Decide the label based on trained tree (API)
	 * 
	 * @param record
	 *            record to test
	 * @return label value (true <-> 1, false <-> 0)
	 */
	public boolean decide(boolean[] record) {
		return decide(root, record);
	}

	/**
	 * Decide the label based on trained tree on a node (for internal)
	 * 
	 * @param n
	 *            current node
	 * @param record
	 *            record to test
	 * @return label value (true <-> 1, false <-> 0)
	 */
	private boolean decide(Node n, boolean[] record) {
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
	private void trainNode(Node n, List<boolean[]> records,
			Set<Integer> recordIds, Set<Integer> unusedFeatures) {
		// end condition
		if (n.pos == 0 || n.neg == 0 || unusedFeatures.isEmpty()) {
			return;
		}

		double maxMI = 0.0; // maximum mutual information
		int maxId = -1; // id of the feature that maximize MI
		n.left = new Node(null);
		n.right = new Node(null);

		for (int i : unusedFeatures) { // check all possible features
			n.featureName = features.get(i);

			n.left.pos = 0;
			n.left.neg = 0;
			n.right.pos = 0;
			n.right.neg = 0;

			for (int id : recordIds) { // divide records to left and right
										// subtrees
				boolean[] record = records.get(id);
				int len = record.length;
				if (record[i]) { // assign records with positive feature value
									// to left subtree
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

		if (maxMI <= 0) { // no information gain
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

		trainNode(n.left, records, leftRecordIds, new HashSet<>(unusedFeatures));
		trainNode(n.right, records, rightRecordIds, new HashSet<>(
				unusedFeatures));
	}

	/*
	 * Represent a tree node
	 */
	private class Node {
		/*
		 * name of feature assigned to this node
		 */
		String featureName;

		/*
		 * number of positive labels
		 */
		int pos;

		/*
		 * number of negative labels
		 */
		int neg;

		/*
		 * left child, with feature value in parent positive
		 */
		Node left;

		/*
		 * right child, with feature value in parent negative
		 */
		Node right;

		public Node(String featureName) {
			this.featureName = featureName;
			pos = 0;
			neg = 0;
		}

		/**
		 * Calculate entropy of the node
		 *
		 * @return entropy of the node
		 */
		public double entropy() {
			if (pos == 0 || neg == 0)
				return 0.0;

			double res = pos / (double) (pos + neg)
					* Math.log(pos / (double) (pos + neg)) / Math.log(2);
			res += neg / (double) (pos + neg)
					* Math.log(neg / (double) (pos + neg)) / Math.log(2);
			return -res;
		}

		/**
		 * calculate the mutual information (information gain)
		 * 
		 * @return current mutual information of this node
		 */
		public double mutualInformation() {
			int total = pos + neg;
			double mi = entropy() - (left.neg + left.pos) / (double) total
					* left.entropy() - (right.neg + right.pos) / (double) total
					* right.entropy();

			return mi;
		}

	}
}
