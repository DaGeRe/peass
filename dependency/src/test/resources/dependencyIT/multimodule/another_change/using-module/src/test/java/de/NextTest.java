package de;

import org.junit.Test;
import de.dagere.base.BaseChangeable;
import de.dagere.base.NextBaseChangeable;

public class NextTest {
	@Test
	public void nextTestMe() {
		final BaseChangeable bc = new BaseChangeable();
		bc.doSomething();

		final NextBaseChangeable nbc = new NextBaseChangeable();
		nbc.doSomething();
	}

	@Test
	public void nextTestMeAlso() {
		final NextBaseChangeable nbc = new NextBaseChangeable();
		nbc.doSomething();
	}
}
