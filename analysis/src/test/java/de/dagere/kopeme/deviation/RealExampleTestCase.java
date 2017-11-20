package de.dagere.kopeme.deviation;

import de.dagere.kopeme.junit3.KoPeMeTestcase;
import de.dagere.kopeme.datacollection.DataCollectorList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RealExampleTestCase extends KoPeMeTestcase {
	protected DataCollectorList getDataCollectors() {
		return DataCollectorList.ONLYTIME;
	}

	protected long getMaximalTime() {
		return 120000;
	}

	protected int getWarmupExecutions() {
		return 116;
	}

	protected int getExecutionTimes() {
		return 233;
	}

	protected boolean logFullData() {
		return false;
	}

	protected boolean useKieker() {
		return false;
	}

	public void testFileAccess() {
		File f = new File("test.txt");
		FileWriter fw;
		try {
			fw = new FileWriter(f);
			fw.write("test");
			fw.close();
			BufferedReader fr = new BufferedReader(new FileReader(f));
			String result = fr.readLine();
			System.out.println(result);
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
