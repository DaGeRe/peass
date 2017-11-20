package de.peran.measurement.processinstrumenter;

import java.io.File;

import de.peran.dependency.analysis.data.TestSet;

public interface ProcessInstrumenter {

	void executeTests(TestSet testset, File logFile);
}
