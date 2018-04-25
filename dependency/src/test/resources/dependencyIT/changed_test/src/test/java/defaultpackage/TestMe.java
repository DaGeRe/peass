package defaultpackage;
import org.junit.Test;

import defaultpackage.NormalDependency;
import defaultpackage.OtherDependency;

public class TestMe {	
	
	@Test
	public void testMe(){
		final NormalDependency normal = new NormalDependency();
		normal.executeThing();
		System.out.println("This is only a test change - should be detected, could influence performance!");
	}
	
	@Test
	public void removeMe(){
		final OtherDependency other = new OtherDependency();
		other.executeThing();
	}
}
