package de.dagere.kopeme.deviation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

public class ExampleTestCaseFileAccess extends TestCase {

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
