package prep;

/**
 * This class represents a processed (labeled) data record. Its attributes represent features.
 * 
 * @author zzuo
 *
 */
public class ProcessedDataRecord {
	/*
	 * Number of minutes in the day (e.g. 20090501 01:40:01.009 => 100)
	 * Time in a day could be a factor for foreign exchange price。
	 * TODO: Consider other features, e.g. day in a week
	 */
	private int minutesOfDay;
	
	/*
	 * Minimum bid in the time window
	 */
	private double minBid;
	
	/*
	 * Maximum bid in the time window
	 */
	private double maxBid;
	
	/*
	 * Range of bid in the time window, showing the level of price change.
	 * TODO: Consider introducing standard deviation
	 */
	private double rangeBid;
	
	/*
	 * Difference between "bid" and "ask" value, or so-called "pip" value
	 */
	private double bidAskDiff;
	
	/*
	 * label for data record
	 */
	private int label;

	public ProcessedDataRecord() {
		
	}
	
	public ProcessedDataRecord(int minutesOfDay, double minBid, double maxBid,
			double rangeBid, double bidAskDiff, int label) {
		super();
		this.minutesOfDay = minutesOfDay;
		this.minBid = minBid;
		this.maxBid = maxBid;
		this.rangeBid = rangeBid;
		this.bidAskDiff = bidAskDiff;
		this.label = label;
	}

	public int getMinutesOfDay() {
		return minutesOfDay;
	}

	public void setMinutesOfDay(int minutesOfDay) {
		this.minutesOfDay = minutesOfDay;
	}

	public double getMinBid() {
		return minBid;
	}

	public void setMinBid(double minBid) {
		this.minBid = minBid;
	}

	public double getMaxBid() {
		return maxBid;
	}

	public void setMaxBid(double maxBid) {
		this.maxBid = maxBid;
	}

	public double getRangeBid() {
		return rangeBid;
	}

	public void setRangeBid(double rangeBid) {
		this.rangeBid = rangeBid;
	}

	public double getBidAskDiff() {
		return bidAskDiff;
	}

	public void setBidAskDiff(double bidAskDiff) {
		this.bidAskDiff = bidAskDiff;
	}

	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}
		
}
