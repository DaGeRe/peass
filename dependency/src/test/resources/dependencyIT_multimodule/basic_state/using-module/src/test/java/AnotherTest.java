import org.junit.Test;

import de.dagere.base.BaseChangeable;
import de.dagere.using.AnotherChangeable;

public class AnotherTest {

	@Test
	public void testMe() {
		final AnotherChangeable ac = new AnotherChangeable();
		ac.callMe();
	}

	@Test
	public void testMeAlso() {
		final BaseChangeable bc = new BaseChangeable();
		bc.doSomething();
	}
}
