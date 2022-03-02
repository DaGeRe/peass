package de.dagere.peass.statisticlogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExceededTraceLogger {

    public static void logTestExceedingTraceSize(File kiekerResultFolder, long sizeInMB) {
        int indexThirdLastOccurrence  = nthLastIndexOf(kiekerResultFolder.getParentFile().getPath(), File.separator, 3);
        String testName = kiekerResultFolder.getParentFile().getPath().substring(indexThirdLastOccurrence + 1);

        File traceSizeFile = new File(System.getProperty("user.dir"), "results" + File.separator + "exceedingTraceSize.txt");
        try {
            BufferedWriter writerTraceSize = new BufferedWriter(new FileWriter(traceSizeFile, true));
            writerTraceSize.write(testName);
            writerTraceSize.write(" : " + sizeInMB);
            writerTraceSize.newLine();
            writerTraceSize.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int nthLastIndexOf(String str, String searchStr, int nth) {
        if (nth <= 0) {
            return str.length();
        }

        return nthLastIndexOf(str.substring(0, str.lastIndexOf(searchStr)), searchStr, --nth);
    }
}
