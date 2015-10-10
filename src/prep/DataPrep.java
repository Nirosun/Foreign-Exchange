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

//		writeContinuousRecords(outputFileName, processedRecords);
		
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
	 * Write binary labeled data into an output csv file
	 * 
	 * @param outputFileName
	 *            output file name
	 * @param records
	 *            list of labeled data records
	 */
	public void writeBinaryRecords(String outputFileName,
			List<boolean[]> records) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(
				outputFileName))) {
			writer.write("avg_bid,range_bid,diff_bid,delta_bid,spread,label\n");
			for (boolean[] r : records) {
				StringBuilder sb = new StringBuilder();
				int i;
				for (i = 0; i < r.length - 1; i ++) {
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
	
	private List<boolean[]> binarizeRecords(List<ProcessedDataRecord> contRecords) {
		
		double thresholdAvgBid = 0;
		double thresholdRangeBid = 0;
		double thresholdDiffBid = 0;
		double thresholdDeltaBid = 0;
		double thresholdSpread = 0;
		
		for ( ProcessedDataRecord r : contRecords ) {
			thresholdAvgBid += r.getAvgBid();
			thresholdRangeBid += r.getRangeBid();
			thresholdSpread += r.getSpread();
		}
		
		thresholdAvgBid /= contRecords.size();
		thresholdRangeBid /= contRecords.size();
		thresholdSpread /= contRecords.size();

		List<boolean[]> binaryRecords = new ArrayList<>();
		
		for ( ProcessedDataRecord r : contRecords ) {
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

	public static void main(String[] args) {
		new DataPrep().prepareData("sample_raw_0912.csv", "sample_labeled_0912.csv");
	}
}
