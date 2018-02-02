package de.peran.example;

import java.io.File;

import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.writer.filesystem.ChangeableFolderSyncFsWriter;

public class CallerLongFor {

	int x = 0;

	public static void main(final String[] args) {
		try {
			final File tmpFolder = new File("target/kieker_results_test/");
			ChangeableFolderSyncFsWriter.getInstance(MonitoringController.getInstance()).setFolder(tmpFolder);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		System.out.println("Here it starts");
		final CalleeLongFor c = new CalleeLongFor();
		c.callMe();
	}
}
