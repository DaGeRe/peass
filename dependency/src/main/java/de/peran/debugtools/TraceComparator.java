package de.peran.debugtools;

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
