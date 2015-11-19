package prep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import util.GlobalParams;

/**
 * This class is responsible for prepare data for analysis. It has methods to
 * read raw data records from file, process data, and write labeled data into a
 * new cvs file.
 * 
 * @author zzuo
 *
 */
public class DataPrep {

	private static final long WINDOW_SIZE_IN_MILLIS = 1000 * 60 /* seconds */
			* 5 /* minutes */;

	/**
	 * Prepare data for analysis (reads data, process data, and write data)
	 * 
	 * @param inputFileName
	 *            input file name
	 * @param outputFileName
	 *            output file name
	 */
	public void prepareData(String inputFileName, String outputFileName) {
		List<RawDataRecord> rawRecords = readRecords(inputFileName);
		List<ProcessedDataRecord> processedRecords = new ArrayList<ProcessedDataRecord>();

		// skip first 100 records (initial records may introduce significant
		// error due to less data)
		for (int i = 100; i < rawRecords.size() - 100; i++) {
			ProcessedDataRecord p = processRecord(rawRecords, i);
			processedRecords.add(p);
		}

		writeContinuousRecords("sample_labeled_cont.csv", processedRecords);

		List<boolean[]> binaryRecords = binarizeRecords(processedRecords);

		writeBinaryRecords(outputFileName, binaryRecords);
	}

	/**
	 * Load data records from input file into a list
	 * 
	 * @param inputFileName
	 *            input file name
	 * @return all raw data records
	 */
	public List<RawDataRecord> readRecords(String inputFileName) {
		List<RawDataRecord> records = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormat
				.forPattern("yyyyMMdd HH:mm:ss.SSS");

		try (BufferedReader reader = new BufferedReader(
				new FileReader(inputFileName))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] strs = line.split(",");
				DateTime timestamp = formatter.parseDateTime(strs[1]);
				RawDataRecord r = new RawDataRecord();
				r.setTime(timestamp);
				r.setBid(Double.parseDouble(strs[2]));
				r.setAsk(Double.parseDouble(strs[3]));
				records.add(r);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return records;
	}

	/**
	 * Write binary labeled data into an output csv file
	 * 
	 * @param outputFileName
	 *            output file name
	 * @param records
	 *            list of labeled data records
	 */
	public void writeBinaryRecords(String outputFileName,
			List<boolean[]> records) {
		try (BufferedWriter writer = new BufferedWriter(
				new FileWriter(outputFileName))) {
			writer.write("avg_bid,range_bid,diff_bid,delta_bid,spread,label\n");
			for (boolean[] r : records) {
				StringBuilder sb = new StringBuilder();
				int i;
				for (i = 0; i < r.length - 1; i++) {
					sb.append(r[i]).append(",");
				}
				sb.append(r[i]).append("\n");
				writer.write(sb.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write continuous processed (labeled) data into an output csv file
	 * 
	 * @param outputFileName
	 *            output file name
	 * @param records
	 *            list of labeled data records
	 */
	public void writeContinuousRecords(String outputFileName,
			List<ProcessedDataRecord> records) {
		try (BufferedWriter writer = new BufferedWriter(
				new FileWriter(outputFileName))) {
			writer.write("avg_bid,range_bid,diff_bid,delta_bid,spread,label\n");
			for (ProcessedDataRecord r : records) {
				StringBuilder sb = new StringBuilder();
				sb.append(r.getAvgBid()).append(",").append(r.getRangeBid())
						.append(",").append(r.getDiffBid()).append(",")
						.append(r.getDeltaBid()).append(",")
						.append(r.getSpread()).append(",");
				sb.append(r.getLabel()).append("\n");
				writer.write(sb.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generate a labeled data record corresponding to a raw record. Generate
	 * features and label
	 * 
	 * @param rawRecords
	 *            list of raw data records
	 * @param index
	 *            index of current record in list
	 * @return processed (labeled) data record
	 */
	public ProcessedDataRecord processRecord(List<RawDataRecord> rawRecords,
			int index) {
		RawDataRecord currRecord = rawRecords.get(index - 1);

		ProcessedDataRecord p = new ProcessedDataRecord();
		DateTime currTime = currRecord.getTime();

		double minBid = currRecord.getBid();
		double maxBid = minBid;

		p.setSpread(currRecord.getAsk() - currRecord.getBid());

		// calculate the directionality label
		// find the next bid value that's different with current bid value, and
		// set the label respectively
		int label = Math.random() > 0.5 ? 1 : 0;

		for (int i = index; i < rawRecords.size(); i++) {
			if (rawRecords.get(i).getBid() != currRecord.getBid()) {
				label = rawRecords.get(i).getBid() > currRecord.getBid() ? 1
						: 0;
				break;
			}
		}

		p.setLabel(label);

		double avgBid = 0;

		int i;
		for (i = index - 1; i >= 0; i--) {
			RawDataRecord r = rawRecords.get(i);
			if (r.getTime().isAfter(currTime.minus(WINDOW_SIZE_IN_MILLIS))) {
				maxBid = Math.max(maxBid, r.getBid());
				minBid = Math.min(minBid, r.getBid());
				avgBid += r.getBid();
			} else {
				break;
			}
		}

		avgBid = avgBid / (index - i);

		p.setAvgBid(avgBid);
		p.setRangeBid(maxBid - minBid);
		p.setDiffBid(
				currRecord.getBid() - rawRecords.get(i >= 0 ? i : 0).getBid());
		p.setDeltaBid(currRecord.getBid() - rawRecords.get(index - 2).getBid());

		return p;
	}

	/**
	 * Binarize features values by calculating and applying thresholds
	 * 
	 * @param contRecords
	 *            list of records with features of continuous values
	 * @return list of records with boolean features
	 */
	private List<boolean[]> binarizeRecords(
			List<ProcessedDataRecord> contRecords) {

		double thresholdAvgBid = 0;
		double thresholdRangeBid = 0;
		double thresholdDiffBid = 0;
		double thresholdDeltaBid = 0;
		double thresholdSpread = 0;

		for (ProcessedDataRecord r : contRecords) {
			thresholdAvgBid += r.getAvgBid();
			thresholdRangeBid += r.getRangeBid();
			thresholdSpread += r.getSpread();
		}

		thresholdAvgBid /= contRecords.size();
		thresholdRangeBid /= contRecords.size();
		thresholdSpread /= contRecords.size();

		List<boolean[]> binaryRecords = new ArrayList<>();

		for (ProcessedDataRecord r : contRecords) {
			boolean[] binaries = new boolean[6];

			binaries[0] = r.getAvgBid() > thresholdAvgBid;
			binaries[1] = r.getRangeBid() > thresholdRangeBid;
			binaries[2] = r.getDiffBid() > thresholdDiffBid;
			binaries[3] = r.getDeltaBid() > thresholdDeltaBid;
			binaries[4] = r.getSpread() > thresholdSpread;
			binaries[5] = r.getLabel() == 1;

			binaryRecords.add(binaries);
		}

		return binaryRecords;
	}

	/**
	 * Split labeled data file into training (80%) and testing (20%) data files
	 * 
	 * @param inputFileName
	 * @param trainFileName
	 * @param testFileName
	 */
	public void splitTrainAndTestFiles(String inputFileName,
			String trainFileName, String testFileName) {
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(inputFileName));
			PrintWriter trainWriter = new PrintWriter(
					new FileWriter(trainFileName));
			PrintWriter testWriter = new PrintWriter(
					new FileWriter(testFileName));

			String line = null;

			int id = 0;
			while ((line = reader.readLine()) != null) {
				if (id == 0) {
					trainWriter.println(line);
					testWriter.println(line);
					id++;
					continue;
				}

				if (id % 5 == 0) {
					testWriter.println(line);
				} else {
					trainWriter.println(line);
				}
				id++;
			}

			reader.close();
			trainWriter.close();
			testWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Transmit data from csv file to Cassandra database
	 * 
	 * @param trainFileName
	 *            training data file name
	 * @param testFileName
	 *            testing data file name
	 */
	public void csvToCassandra(String trainFileName, String testFileName) {
		Cluster cluster;
		Session session;

		cluster = Cluster.builder().addContactPoint(GlobalParams.CASSANDRA_ADDR).build();
		session = cluster.connect("test");

		session.execute("DROP TABLE IF EXISTS train_data");
		session.execute("DROP TABLE IF EXISTS test_data");

//		session.execute(
//				"CREATE TABLE train_data (id bigint PRIMARY KEY, avg_bid boolean, range_bid boolean, diff_bid boolean, delta_bid boolean, spread boolean, label boolean)");
//		session.execute(
//				"CREATE TABLE test_data (id bigint PRIMARY KEY, avg_bid boolean, range_bid boolean, diff_bid boolean, delta_bid boolean, spread boolean, label boolean)");

		session.execute(
				"CREATE TABLE train_data (id bigint PRIMARY KEY, avg_bid int, range_bid int, diff_bid int, delta_bid int, spread int, label int)");
		session.execute(
				"CREATE TABLE test_data (id bigint PRIMARY KEY, avg_bid int, range_bid int, diff_bid int, delta_bid int, spread int, label int)");

		
		insertData(trainFileName, true, session);
		insertData(testFileName, false, session);

		cluster.close();
	}

	/**
	 * Insert data into table in Cassandra (for training or testing)
	 * 
	 * @param fileName
	 *            data file (csv) name
	 * @param isTrain
	 *            is the data for training
	 * @param session
	 *            Cassandra session
	 */
	private void insertData(String fileName, boolean isTrain, Session session) {

		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			int id = 1;

			while ((line = br.readLine()) != null) {
				String[] strs = line.split(",");

				StringBuilder sb = new StringBuilder();

				if (isTrain) {
					sb.append(
							"INSERT INTO train_data (id, avg_bid, range_bid, diff_bid, delta_bid, spread, label) VALUES (");
				} else {
					sb.append(
							"INSERT INTO test_data (id, avg_bid, range_bid, diff_bid, delta_bid, spread, label) VALUES (");
				}

				sb.append(id);

				for (int i = 0; i < strs.length; i++) {
					int val = (strs[i].toLowerCase().equals("true")) ? 1 : 0;
					sb.append(", ").append(val);
				}

				sb.append(")");

				session.execute(sb.toString());

				id++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new DataPrep().prepareData("sample_raw.csv", "sample_labeled.csv");

		System.out.println("Labeled data generated.");

		new DataPrep().splitTrainAndTestFiles("sample_labeled.csv",
				"sample_train.csv", "sample_test.csv");

		System.out.println("Train and test files splitted.");

		new DataPrep().csvToCassandra("sample_train.csv", "sample_test.csv");

		System.out.println("Data transmitted into Cassandra.");
	}
}
