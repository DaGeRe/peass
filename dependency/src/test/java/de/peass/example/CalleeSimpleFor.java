package de.peass.example;

public class CalleeSimpleFor {

	public void callMe() {
		for (int i = 0; i < 10; i++) {
			callMe2();
		}
	}

	private void callMe2() {

	}

	/**
	 * This method exists, so it can be parsed as part of an artificial trace
	 */
	public void methodA() {

	}

	/**
	 * This method exists, so it can be parsed as part of an artificial trace
	 */
	public void methodB() {

	}

	/**
	 * This method exists, so it can be parsed as part of an artificial trace
	 */
	public void methodC() {

	}
}
