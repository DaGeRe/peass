package demo.project.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ExampleTest {

   @Test
   public void test() {
      final ExampleClass exampleClazz = new ExampleClass();
      exampleClazz.calleeMethod();
      assertNotNull(exampleClazz);
      assertEquals(5, senselessClazz.staticMethod());
   }

   public static class senselessClazz {
      private static int senselessInt = 5;

      public static int staticMethod() {
         return senselessInt;
      }
   }

}
