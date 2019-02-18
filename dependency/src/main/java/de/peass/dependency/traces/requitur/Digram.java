package de.peass.dependency.traces.requitur;

class Digram {
	private final Symbol start, end;
	Rule rule;

	public Digram(Symbol start, Symbol end) {
		super();
		this.start = start;
		this.end = end;
	}

	public Symbol getStart() {
		return start;
	}

	public Symbol getEnd() {
		return end;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	@Override
	public int hashCode() {
		return start.hashCode() + end.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Digram) {
			Digram other = (Digram) obj;
			return start.equals(other.start) && end.equals(other.end);
		}
		return false;
	}

	@Override
	public String toString() {
		return start.toString() + " " + end.toString();
	}
}