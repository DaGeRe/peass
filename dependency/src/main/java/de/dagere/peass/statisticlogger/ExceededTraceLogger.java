package de.dagere.peass.statisticlogger;

import de.dagere.peass.dependency.analysis.data.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExceededTraceLogger {

    public static void logTestExceedingTraceSize(TestCase testCase, long sizeInMB) {
        File traceSizeFile = new File(System.getProperty("user.dir"), "results" + File.separator + "exceedingTraceSize.txt");
        try {
            BufferedWriter writerTraceSize = new BufferedWriter(new FileWriter(traceSizeFile, true));
            writerTraceSize.write(testCase.getClazz() + "#" + testCase.getMethod());
            writerTraceSize.write(" : " + sizeInMB);
            writerTraceSize.newLine();
            writerTraceSize.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
