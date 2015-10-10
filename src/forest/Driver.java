package forest;

public class Driver {
	public static void main(String[] args) {
		int N = 10;
		
		long startTime = System.currentTimeMillis();
		
		RandomForest forest = new RandomForest(N);
		
		String trainFile = "sample_train.csv";
		String testFile = "sample_test.csv";
		
		System.out.println("Training: ");
		forest.train(trainFile);
		
		System.out.println("\nTesting: ");

		forest.test(testFile);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("Time elapsed: " + (endTime - startTime) + " ms");
	}
}
