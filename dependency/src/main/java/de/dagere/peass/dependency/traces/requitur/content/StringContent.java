package de.dagere.peass.dependency.traces.requitur.content;

public class StringContent extends Content {
	private final String value;

	public StringContent(final String value) {
		super();
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof StringContent) {
			return value.equals(((StringContent) obj).value);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value;
	}
}