package dtree;

import java.io.Serializable;

/**
 * This class represents a node in the desicion tree
 * 
 * @author zzuo
 *
 */
public class TreeNode implements Serializable {

	private static final long serialVersionUID = 4941084880435568369L;

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
	TreeNode left;

	/*
	 * right child, with feature value in parent negative
	 */
	TreeNode right;

	public TreeNode(String featureName) {
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
