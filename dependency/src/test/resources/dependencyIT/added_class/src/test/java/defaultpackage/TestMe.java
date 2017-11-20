package defaultpackage;

import org.junit.Test;

public class TestMe {
	
	@Test
	public void testMe(){
		NormalDependency normal = new NormalDependency();
		normal.executeThing();
		System.out.println("Test1");
	}
}
