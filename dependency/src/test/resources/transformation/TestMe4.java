/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import de.dagere.kopeme.junit.testrunner.PerformanceTestRunnerJUnit;
import de.dagere.kopeme.annotations.Assertion;
import de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation;
import org.junit.rules.TestRule;
import org.junit.Rule;
import de.dagere.kopeme.junit4.rule.KoPeMeRule;

public class TestMe4 {

    @Test
    @de.dagere.kopeme.annotations.PerformanceTest(executionTimes = 1, warmupExecutions = 0, logFullData = true, timeout = 60000, repetitions = 1, dataCollectors = "ONLYTIME")
    public void testMethod1() {
        System.out.println("This is tested.");
    }

    @Rule()
    public TestRule kopemeRule = new KoPeMeRule(this);

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public static TemporaryFolder testFolder2 = new TemporaryFolder();
}
