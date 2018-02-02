package de.peran.example;

public class CalleeLongFor {

	private int j = 1;
	
	public void callMe() {
		for (int i = 0; i < 10; i++) {
			callMe2();
			callMe3();
		}
	}

	private void callMe2() {

	}

	private void callMe3() {
		j = callMe4()+2;
	}
	
	private int callMe4() {
		return j+1;
	}

}
