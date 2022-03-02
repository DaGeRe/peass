package de.dagere.peass.statisticlogger;

import de.dagere.peass.dependency.analysis.data.CalledMethods;
import de.dagere.peass.dependency.analysis.data.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class IncludedTestsLogger {

    public static void logIncludedTests(Map<TestCase, CalledMethods> dependencyMap){
        File includedTestsFile = new File(System.getProperty("user.dir"), "results" + File.separator + "includedTests.txt");
        try {
            BufferedWriter writerIncludedTests = new BufferedWriter(new FileWriter(includedTestsFile, false));
            for (TestCase testcase : dependencyMap.keySet()) {
                writerIncludedTests.write(testcase.getClazz() + "#" + testcase.getMethod());
                writerIncludedTests.newLine();
            }
            writerIncludedTests.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
