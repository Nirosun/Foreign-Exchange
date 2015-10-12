package forest;

/**
 * This class is for illustrating the use of the RandomForest class
 * 
 * @author zzuo
 *
 */
public class Driver {
	public static void main(String[] args) {
		// number of trees to grow
		int N = 30;

		long startTime = System.currentTimeMillis();

		// create a random forest
		RandomForest forest = new RandomForest(N);

		String trainFile = "sample_train.csv";
		String testFile = "sample_test.csv";

		System.out.println("Training: ");

		forest.train(trainFile);

		System.out.println("\nSerializing forest into file...");

		RandomForest.serialize(forest, "forest.ser");

		System.out.println("\nDeserializing forest from file...");

		forest = RandomForest.deserialize("forest.ser");

		System.out.println("\nTesting: ");

		forest.test(testFile);

		long endTime = System.currentTimeMillis();

		System.out.println("\nTime elapsed: " + (endTime - startTime) + " ms");
	}
}
