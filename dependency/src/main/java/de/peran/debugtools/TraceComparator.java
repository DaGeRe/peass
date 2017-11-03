package de.peran.debugtools;

/*-
 * #%L
 * peran-dependency
 * %%
 * Copyright (C) 2017 DaGeRe
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


import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.peran.utils.StreamGobbler;

/**
 * Compares two traces, given in as command line parameters, for debugging purposes
 * 
 * @author reichelt
 *
 */
public class TraceComparator {
	public static void main(final String[] args) throws IOException, InterruptedException {
		final File folder = new File(args[0]);
		final String methodName = args[1];

		final File compareFolder = new File("compareFolder");
		if (!compareFolder.exists()) {
			compareFolder.mkdir();
		}

		final File current = new File(".");
		System.out.println("Current dir:" + current);

		final Process exec = Runtime.getRuntime().exec("bash -c 'pwd'");
		StreamGobbler.showFullProcess(exec);

		int fileIndex = 0;
		for (final File file : FileUtils.listFiles(folder, new WildcardFileFilter("kieker*.dat"), TrueFileFilter.INSTANCE)) {
			if (file.getAbsolutePath().contains(methodName)) {
				final File destFile = new File(compareFolder, file.getName());
				FileUtils.copyFile(file, destFile);
				final String command = "cat " + destFile.getAbsolutePath() + " | grep \"org.apache.commons.io\" | cut -d \";\" -f3 > compareFolder/file_" + fileIndex + "";
				System.out.println(command);
				fileIndex++;
			}
		}
		for (int i = 0; i < fileIndex - 1; i++) {
			System.out.println("diff -y -W 200 compareFolder/file_" + i + " compareFolder/file_" + (i + 1));
		}
	}
}
