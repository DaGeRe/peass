package defaultpackage.mypackage;

import org.junit.Test;

import defaultpackage.NormalDependency;
import defaultpackage.OtherDependency;

public class TestMe {	
	
	@Test
	public void testMe(){
		final NormalDependency normal = new NormalDependency();
		normal.executeThing();
		System.out.println("Test1");
	}
	
	@Test
	public void justAnotherTestMethod(){
		final OtherDependency other = new OtherDependency();
		other.executeThing();
	}
}
