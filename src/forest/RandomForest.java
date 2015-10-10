package forest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dtree.DecisionTree;

public class RandomForest {
	private List<DecisionTree> trees;
	
	private int N;
	
	private int sqrtNumOfFeatures;
	
	private int numOfTrainRecords;
	
	private List<String> features;
	
	public RandomForest(int N) {
		this.N = N;
		this.trees = new ArrayList<>();
		this.features = new ArrayList<>();
	}
	
	public void train(String trainFileName) {
		List<boolean[]> records = readInRecords(trainFileName, true /* is training */);
		
		sqrtNumOfFeatures = (int)Math.sqrt(features.size());
		numOfTrainRecords = (int)(0.667 * records.size());
		
		// grow N trees
		for (int i = 0; i < N; i ++) {
			DecisionTree tree = new DecisionTree(features);
			trees.add(tree);
			
			Set<Integer> featureIds = selectFeatures();
			
			List<boolean[]> trainRecords = new ArrayList<>();
			List<boolean[]> testRecords = new ArrayList<>();
						
			splitTrainAndTestRecords(records, trainRecords, testRecords);
						
			tree.train(trainRecords, featureIds);
			
			double errRate = test(testRecords);
			
			System.out.println((i + 1) + " trees, error rate: " + errRate);
		}		
	}
	
	/**
	 * Test (API)
	 * @param testFileName
	 */
	public void test(String testFileName) {
		List<boolean[]> records = readInRecords(testFileName, false /* is testing */);
		
		double errRate = test(records);
		
		System.out.println("Test error rate: " + errRate);
	}
	
	/**
	 * Test (internal use)
	 * @param records
	 */
	private double test(List<boolean[]> records) {
		int errCnt = 0;
		
		for (boolean[] r : records) {
			boolean decision = decide(r);
			
			if (decision != r[r.length - 1]) {
				errCnt ++;
			}
		}
		
		double errRate = (double)errCnt / records.size();
		
		return errRate;
	}
	
	/**
	 * Make decision on a specific record
	 * @param record
	 * @return
	 */
	public boolean decide(boolean[] record) {		
		int pos = 0;
		int neg = 0;
		
		for (int i = 0; i < trees.size(); i ++) {
			boolean des = trees.get(i).decide(record);
			if (des) {
				pos ++;
			} else {
				neg ++;
			}
		}
				
		return pos >= neg;
	}
	
	public static void serialize(RandomForest forest, String fileName) {
		
	}
	
	public static RandomForest deserialize(String fileName) {
		return null;
	}
	
	private Set<Integer> selectFeatures() {
		Set<Integer> featureIds = new HashSet<>();
				
		int n = features.size();
		
		for (int i = 0; i < sqrtNumOfFeatures; i ++) {
			int id;
			do {
				id = (int) (Math.random() * n);
			} while (featureIds.contains(id));
			featureIds.add(id);
		}
		
		return featureIds;
	}
	
	private void splitTrainAndTestRecords(List<boolean[]> allRecords, 
			List<boolean[]> trainRecords, List<boolean[]> testRecords) {		
		Set<Integer> trainIds = new HashSet<>();
		
		for (int i = 0; i < numOfTrainRecords; i ++) {
			int id = (int)(Math.random() * allRecords.size());
			trainRecords.add(allRecords.get(id));
			trainIds.add(id);
		}
		
		for (int i = 0; i < allRecords.size(); i ++) {
			if (!trainIds.contains(i)) {
				testRecords.add(allRecords.get(i));
			}
		}
	}
	
	private List<boolean[]> readInRecords(String fileName, boolean isTrain) {
		List<boolean[]> records = new ArrayList<>(); // records in
		// boolean format
		
		// read in records from training file
		try (BufferedReader br = new BufferedReader(new FileReader(
				fileName))) {
			String line = br.readLine();
			if (isTrain) {
				features.addAll(Arrays.asList(line.split(",")));
				features.remove(features.size() - 1);
			}

			while ((line = br.readLine()) != null) {
				String[] strs = line.split(",");
				int len = strs.length;
				boolean[] binaries = new boolean[len];
				for (int i = 0; i < len; i++) {
					binaries[i] = Boolean.parseBoolean(strs[i]);
				}
				records.add(binaries);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return records;
	}
}
