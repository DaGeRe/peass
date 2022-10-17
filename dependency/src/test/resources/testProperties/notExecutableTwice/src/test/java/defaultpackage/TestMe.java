package defaultpackage;
import org.junit.Test;

import defaultpackage.NormalDependency;

public class TestMe {	
	
	@Test
	public void testMe(){
		final NormalDependency normal = new NormalDependency();
		normal.executeThing();
		System.out.println("Test1");
	}
}
