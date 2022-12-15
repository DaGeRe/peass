package de.dagere.peass;

import de.dagere.peass.Station;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(SAME_THREAD)
public class ExampleTest {

   private static final Station station = mock(de.dagere.peass.Station.class);

   @BeforeAll
   static void initializeMocks() {
      when(station.get()).thenReturn("MockedStation");
   }

   @Test
   public void test() {
      String result = station.get();
      Assertions.assertEquals("MockedStation", result);
   }
}
