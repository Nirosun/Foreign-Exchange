package forest;

import com.datastax.driver.core.Cluster;

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

		Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1")
				.build();

		System.out.println("Training: ");

		forest.train(cluster);

		System.out.println("\nTesting: ");

		forest.test(cluster);

		cluster.close();

		long endTime = System.currentTimeMillis();

		System.out.println("\nTime elapsed: " + (endTime - startTime) + " ms");
	}
}
