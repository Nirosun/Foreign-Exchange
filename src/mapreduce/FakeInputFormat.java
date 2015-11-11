package mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;

import util.GlobalParams;

/**
 * This class represents a input format that fakes input splits to ensure that
 * the number of mappers equals to the number of decision trees in the random
 * forest
 * 
 * @author zzuo
 *
 */
public class FakeInputFormat extends FileInputFormat<LongWritable, Text> {
	@Override
	public RecordReader<LongWritable, Text> createRecordReader(InputSplit split,
			TaskAttemptContext context) throws IOException {
		context.setStatus(split.toString());
		return new LineRecordReader();
	}

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		List<InputSplit> splits = new ArrayList<InputSplit>();

		for (int i = 0; i < GlobalParams.TREES; i++) {
			splits.add(new InputSplit() {
				@Override
				public long getLength()
						throws IOException, InterruptedException {
					return 0;
				}

				@Override
				public String[] getLocations()
						throws IOException, InterruptedException {
					return new String[] { "" };
				}
			});
		}
		return splits;
	}

}
