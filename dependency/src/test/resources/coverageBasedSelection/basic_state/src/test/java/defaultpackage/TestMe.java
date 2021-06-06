package defaultpackage;
import org.junit.Test;

import defaultpackage.NormalDependency;
import defaultpackage.OtherDependency;

public class TestMe {	
	
	@Test
	public void testFirst(){
		final NormalDependency normal = new NormalDependency();
		normal.executeThing();
		System.out.println("Test1");
	}
	
	@Test
	public void testSecond(){
		final NormalDependency normal = new NormalDependency();
		normal.executeThing();
		normal.executeThing();
		normal.executeThing();
		System.out.println("Test2");
	}
	
	@Test
	public void testThird(){
		final NormalDependency normal = new NormalDependency();
		normal.executeThing();
		System.out.println("Test3");
	}
}
