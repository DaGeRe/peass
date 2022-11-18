package de.dagere.peass;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.mockito.Mockito.mockStatic;

@Execution(SAME_THREAD)
public class ExampleTest {

    @BeforeAll
    static void initializeMocks() {
        MockedStatic<Station> station = mockStatic(Station.class);
        station.when(()-> Station.getStation()).thenReturn("MockedStation");
    }
    @Test
    public void test() {
        final Callee exampleClazz = new Callee();
        String result = exampleClazz.method1();
        Assertions.assertEquals("MockedStation",result);
    }

}
