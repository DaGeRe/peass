package de.dagere.kopeme.deviation;

/*-
 * #%L
 * peran-analysis
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


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
