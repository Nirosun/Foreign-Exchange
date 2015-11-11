package mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import util.GlobalParams;

/**
 * The driver class for running MapReduce
 * 
 * @author zzuo
 *
 */
public class RandomForestMR {

	/**
	 * Create validation (during training) or testing performance table
	 * 
	 * @param cluster
	 *            Cassandra cluster
	 * @param isTrain
	 *            is this for training
	 */
	private static void createPerformanceTable(Cluster cluster,
			boolean isTrain) {
		Session session = cluster.connect("test");

		String tableName = isTrain ? "validation_perf" : "test_perf";

		session.execute("DROP TABLE IF EXISTS " + tableName);
		session.execute("CREATE TABLE " + tableName
				+ " (trees int PRIMARY KEY, accuracy double)");
	}

	public static void main(String[] args) throws Exception {
		Cluster cluster = Cluster.builder()
				.addContactPoint(GlobalParams.CASSANDRA_ADDR).build();

		createPerformanceTable(cluster, true);

		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf, "random forest");
		job.setJarByClass(RandomForestMR.class);
		job.setMapperClass(RandomForestMapper.class);
		job.setCombinerClass(RandomForestReducer.class);
		job.setReducerClass(RandomForestReducer.class);
		job.setInputFormatClass(FakeInputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(1);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.waitForCompletion(true);
	}
}
