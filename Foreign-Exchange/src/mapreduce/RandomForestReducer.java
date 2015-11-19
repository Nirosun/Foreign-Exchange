package mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * The reducer class, receives a collection of json texts of trees, and combine
 * them together to form a json array
 * 
 * @author zzuo
 *
 */
public class RandomForestReducer extends Reducer<Text, Text, Text, Text> {
	public void reduce(Text key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {
		List<String> strs = new ArrayList<>();

		for (Text t : values) {
			strs.add(t.toString());
		}

		StringBuilder sb = new StringBuilder("[");

		for (int i = 0; i < strs.size() - 1; i++) {
			sb.append(strs.get(i)).append(", ");
		}
		sb.append(strs.get(strs.size() - 1)).append("]");

		context.write(new Text(), new Text(sb.toString()));
	}
}
