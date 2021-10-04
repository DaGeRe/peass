package de.dagere.peass;

import org.junit.Test;
import de.dagere.kopeme.annotations.Assertion;
import de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation;
import org.junit.rules.TestRule;
import org.junit.Rule;
import de.dagere.kopeme.junit.rule.KoPeMeRule;

public class MainTest {

    @Test
    @de.dagere.kopeme.annotations.PerformanceTest(iterations = 20, warmup = 0, executeBeforeClassInMeasurement = false, logFullData = true, useKieker = true, timeout = 1200000, repetitions = 100, redirectToNull = true, showStart = false, dataCollectors = "ONLYTIME_NOGC")
    public void testMe() {
        C0_0 object = new C0_0();
        object.method0();
    }

    @Rule()
    public TestRule kopemeRule = new KoPeMeRule(this);
}
