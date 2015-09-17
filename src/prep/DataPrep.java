package prep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * This class is responsible for prepare data for analysis. It has methods to
 * read raw data records from file, process data, and write labeled data into a
 * new cvs file.
 * 
 * @author zzuo
 *
 */
public class DataPrep {

	private static final long WINDOW_SIZE_IN_MILLIS = 1000 * 60 /* seconds */* 5 /* minutes */;

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

		for (int i = 0; i < rawRecords.size(); i++) {
			ProcessedDataRecord p = processRecord(rawRecords, i);
			processedRecords.add(p);
		}

		writeRecords(outputFileName, processedRecords);
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

		try (BufferedReader reader = new BufferedReader(new FileReader(
				inputFileName))) {
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
	 * Write processed (labeled) data into an output csv file
	 * 
	 * @param outputFileName
	 *            output file name
	 * @param records
	 *            list of labeled data records
	 */
	public void writeRecords(String outputFileName,
			List<ProcessedDataRecord> records) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(
				outputFileName))) {
			for (ProcessedDataRecord r : records) {
				StringBuilder sb = new StringBuilder();
				sb.append(r.getMinutesOfDay()).append(",")
						.append(r.getMinBid()).append(",")
						.append(r.getMaxBid()).append(",")
						.append(r.getRangeBid()).append(",")
						.append(r.getBidAskDiff()).append(",");
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
		RawDataRecord currRecord = rawRecords.get(index);
		ProcessedDataRecord p = new ProcessedDataRecord();
		DateTime currTime = currRecord.getTime();

		double minBid = currRecord.getBid();
		double maxBid = minBid;

		p.setMinutesOfDay(currRecord.getTime().getMinuteOfDay());
		p.setBidAskDiff(currRecord.getAsk() - currRecord.getBid());

		// calculate the directionality label
		int label = (index == 0 || currRecord.getBid() <= rawRecords.get(
				index - 1).getBid()) ? 0 : 1;

		p.setLabel(label);

		for (int i = index - 1; i >= 0; i--) {
			RawDataRecord r = rawRecords.get(i);
			if (r.getTime().isAfter(currTime.minus(WINDOW_SIZE_IN_MILLIS))) {
				maxBid = Math.max(maxBid, r.getBid());
				minBid = Math.min(minBid, r.getBid());
			} else {
				break;
			}
		}

		p.setMaxBid(maxBid);
		p.setMinBid(minBid);
		p.setRangeBid(maxBid - minBid);

		return p;
	}

	public static void main(String[] args) {
		new DataPrep().prepareData("EURUSD-2009-05-sample.csv", "output.csv");
	}
}
