package de.peran.dependency.traces.requitur.content;

public class RuleContent extends Content {

	private final String value;
	private int count = 2;

	public RuleContent(final String value) {
		super();
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof RuleContent) {
			return value.equals(((RuleContent) obj).value);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value + " (" + count + ")";
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
