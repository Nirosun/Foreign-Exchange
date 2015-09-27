package prep;

/**
 * This class represents a processed (labeled) data record. Its attributes represent features.
 * 
 * @author zzuo
 *
 */
public class ProcessedDataRecord {
	
	/*
	 * Average bid in the time window
	 */
	private double avgBid;
	
	/*
	 * Range of bid (max - min) in the time window, showing the level of price change.
	 */
	private double rangeBid;
	
	/*
	 * Difference between bid values of last record and first record in window
	 */
	private double diffBid;
	
	/*
	 * Difference between "bid" and "ask" value, or so-called "pip" value
	 */
	private double spread;
	
	/*
	 * Difference between bid values of current record and last record
	 */
	private double deltaBid;
	
	/*
	 * label for data record
	 */
	private int label;

	public ProcessedDataRecord() {
		
	}
	
	public ProcessedDataRecord(double avgBid, double rangeBid, double diffBid,
			double spread, double deltaBid, int label) {
		super();
		this.avgBid = avgBid;
		this.rangeBid = rangeBid;
		this.diffBid = diffBid;
		this.spread = spread;
		this.deltaBid = deltaBid;
		this.label = label;
	}


	public double getAvgBid() {
		return avgBid;
	}


	public void setAvgBid(double avgBid) {
		this.avgBid = avgBid;
	}


	public double getRangeBid() {
		return rangeBid;
	}


	public void setRangeBid(double rangeBid) {
		this.rangeBid = rangeBid;
	}


	public double getDiffBid() {
		return diffBid;
	}


	public void setDiffBid(double diffBid) {
		this.diffBid = diffBid;
	}


	public double getSpread() {
		return spread;
	}


	public void setSpread(double spread) {
		this.spread = spread;
	}


	public double getDeltaBid() {
		return deltaBid;
	}


	public void setDeltaBid(double deltaBid) {
		this.deltaBid = deltaBid;
	}

	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}
		
}
