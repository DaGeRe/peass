package de.dagere.peass.statisticlogger;

import de.dagere.peass.dependency.analysis.data.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ForbiddenMethodsLogger {
    
    public static void logTestContainingForbiddenMethod(TestCase testcase) {
        File integrationTests = new File(System.getProperty("user.dir"), "results" + File.separator + "containingForbiddenMethod.txt");
        try {
            BufferedWriter writerIntegrationTests = new BufferedWriter(new FileWriter(integrationTests, true));
            writerIntegrationTests.write(testcase.getClazz() + "#" + testcase.getMethod());
            writerIntegrationTests.newLine();
            writerIntegrationTests.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
