package de.peran.measurement.analysis.knowledge;

public class Change {
	String diff;
	String clazz;
	double changePercent;
	String correctness;

	public String getCorrectness() {
		return correctness;
	}

	public void setCorrectness(String correctness) {
		this.correctness = correctness;
	}

	String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDiff() {
		return diff;
	}

	public void setDiff(String diff) {
		this.diff = diff;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public double getChangePercent() {
		return changePercent;
	}

	public void setChangePercent(double changePercent) {
		this.changePercent = changePercent;
	}
}