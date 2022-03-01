import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ExampleTestJUnit5Parameterized {

   @Test
   public void checkSomething() throws Exception {
      
   }
   
   @Test
   public void checkSomethingElse() throws Exception {
      
   }
   
   @ParameterizedTest
   @ValueSource(ints = { 0, 1, 2 })
   public void testMe(int value){
   	System.out.println("Executing " + value);
	final NormalDependency normal = new NormalDependency();
	normal.executeThing(value);
   }
}
