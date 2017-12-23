package de.peran.analysis.knowledge;

/**
 * Saves information about one change
 * 
 * @author reichelt
 *
 */
public class Change {
	private String diff;
	private String clazz;
	private double changePercent;
	private double tvalue;
	private String correctness;
	private String type;

	public String getCorrectness() {
		return correctness;
	}

	public void setCorrectness(final String correctness) {
		this.correctness = correctness;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getDiff() {
		return diff;
	}

	public void setDiff(final String diff) {
		this.diff = diff;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(final String clazz) {
		this.clazz = clazz;
	}

	public double getChangePercent() {
		return changePercent;
	}

	public void setChangePercent(final double changePercent) {
		this.changePercent = changePercent;
	}

	public double getTvalue() {
		return tvalue;
	}

	public void setTvalue(double tvalue) {
		this.tvalue = tvalue;
	}
}