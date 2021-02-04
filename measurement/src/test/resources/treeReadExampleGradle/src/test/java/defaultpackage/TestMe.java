package defaultpackage;

import org.junit.Test;

import defaultpackage.NormalDependency;

public class TestMe {

   @Test
   public void testMe() {
      final NormalDependency normal = new NormalDependency();
      normal.executeThing();
      normal.other();
      System.out.println("Test1");
      final OtherDependency other = new Subclass();
      other.executeThing();
      other.other();
      System.out.println("Test1");
   }

}
