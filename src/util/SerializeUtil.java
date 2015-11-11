package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;

import dtree.DecisionTree;
import forest.RandomForest;

/**
 * Utilities for serialization
 * 
 * @author zzuo
 *
 */
public class SerializeUtil {
	/**
	 * Serialize forest into a file
	 * 
	 * @param forest
	 * @param fileName
	 */
	public static void serializeRandomForest(RandomForest forest,
			String fileName) {
		try {
			PrintWriter writer = new PrintWriter(fileName);

			Gson gson = new Gson();

			writer.print(gson.toJson(forest));

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deserialize a RandomForest object from a file
	 * 
	 * @param fileName
	 * @return
	 */
	public static RandomForest deserializeRandomForest(String fileName) {
		RandomForest forest = null;

		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(fileName));

			String json = reader.readLine();

			Gson gson = new Gson();

			forest = gson.fromJson(json, RandomForest.class);

			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return forest;
	}

	public static String decisionTreeToJson(DecisionTree dt) {
		Gson gson = new Gson();
		return gson.toJson(dt);
	}

}
