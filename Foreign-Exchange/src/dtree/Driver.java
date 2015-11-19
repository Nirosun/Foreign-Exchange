package dtree;

import java.util.ArrayList;

/**
 * Illustrate use of DecisionTree class
 * 
 * @author zzuo
 *
 */
public class Driver {
	public static void main(String[] args) {
		String trainFile = "sample_train.csv";
		String testFile = "sample_test.csv";

		DecisionTree tree = new DecisionTree(new ArrayList<String>());

		tree.train(trainFile);

		double err = tree.test(testFile);

		System.out.println("Test error: " + err + ", accuracy: " + (1 - err));
	}
}
