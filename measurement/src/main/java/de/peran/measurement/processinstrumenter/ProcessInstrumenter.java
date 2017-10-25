package de.peran.measurement.processinstrumenter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.peran.dependency.analysis.data.TestSet;
import de.peran.testtransformation.JUnitTestTransformer;

public interface ProcessInstrumenter {

	void executeTests(TestSet testset, File logFile);
}
