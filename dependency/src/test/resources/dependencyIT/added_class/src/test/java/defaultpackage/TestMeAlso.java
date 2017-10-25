package defaultpackage;

import org.junit.Test;

public class TestMeAlso {
	@Test
	public void testMe(){
		Dep dep = new Dep();
		dep.callMe();
	}
}
