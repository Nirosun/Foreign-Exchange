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

		// skip first 100 records (initial records may introduce significant error due to less data)
		for (int i = 100; i < rawRecords.size(); i++) {
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
		RawDataRecord nextRecord = rawRecords.get(index);

		ProcessedDataRecord p = new ProcessedDataRecord();
		DateTime currTime = currRecord.getTime();

		double minBid = currRecord.getBid();
		double maxBid = minBid;

		p.setSpread(currRecord.getAsk() - currRecord.getBid());

		// calculate the directionality label
		int label = nextRecord.getBid() >= currRecord.getBid() ? 1 : 0;

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
		p.setDiffBid(currRecord.getBid() - rawRecords.get(i >= 0 ? i : 0).getBid());
		p.setDeltaBid(currRecord.getBid() - rawRecords.get(index - 2).getBid());

		return p;
	}

	public static void main(String[] args) {
		new DataPrep().prepareData("sample_raw.csv", "sample_labeled.csv");
	}
}
