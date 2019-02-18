package de.peran.reading;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

class MyStuff {
   static void printTest() {
      System.out.println("Test");
   }
}

@RunWith(PowerMockRunner.class)
@PrepareForTest( MyStuff.class )
public class TestMockStatic {

   @Test
   public void testMe() {
      PowerMockito.mockStatic(MyStuff.class);
      
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            System.out.println("Changed!");
            return null;
         }
      }).when(MyStuff.class);
      MyStuff.printTest();
      
//      PowerMockito.when(MyStuff.printTest()).
      
      MyStuff.printTest();
   }
}
