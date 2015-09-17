package prep;

import org.joda.time.DateTime;

/**
 * This class represents a raw data record.
 * 
 * @author zzuo
 *
 */
public class RawDataRecord {

	private DateTime time;

	private double bid;

	private double ask;

	public RawDataRecord() {

	}

	public RawDataRecord(DateTime time, double bid, double ask) {
		super();
		this.time = time;
		this.bid = bid;
		this.ask = ask;
	}

	public DateTime getTime() {
		return time;
	}

	public void setTime(DateTime time) {
		this.time = time;
	}

	public double getBid() {
		return bid;
	}

	public void setBid(double bid) {
		this.bid = bid;
	}

	public double getAsk() {
		return ask;
	}

	public void setAsk(double ask) {
		this.ask = ask;
	}

}
