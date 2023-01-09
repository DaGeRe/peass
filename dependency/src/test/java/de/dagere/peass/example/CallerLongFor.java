package de.dagere.peass.example;

import java.io.File;

import de.dagere.kopeme.kieker.writer.ChangeableFolderWriter;

public class CallerLongFor {

	int x = 0;

	public static void main(final String[] args) {
		try {
			final File tmpFolder = new File("target/kieker_results_test/");
			ChangeableFolderWriter.getInstance().setFolder(tmpFolder);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		System.out.println("Here it starts");
		final CalleeLongFor c = new CalleeLongFor();
		c.callMe();
	}
}
