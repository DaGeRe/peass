package de.peran.measurement.analysis.statistics;

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


import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependencyprocessors.VersionComparator;

/**
 * Saves the measurement data of one testclass in every version and every run.
 * 
 * @author reichelt
 *
 */
public class TestData {

	private static final Logger LOG = LogManager.getLogger(TestData.class);

	private final TestCase testcase;

	private final SortedMap<String, EvaluationPair> data = new TreeMap<>(VersionComparator.INSTANCE);

	public TestData(final TestCase testcase) {
		super();
		this.testcase = testcase;
	}

	public void addMeasurement(final String version, final Kopemedata resultData, final boolean isNew) {
		final String versionOfPair = isNew ? VersionComparator.getNextVersionForTestcase(testcase, version) : version;

		LOG.trace("Pair-Version: {} Class: {} Method: {}", versionOfPair, testcase.getClazz(), testcase.getMethod());
		EvaluationPair currentPair = data.get(versionOfPair);
		// LOG.debug(currentPair);
		if (currentPair == null) {
			final String predecessor = isNew ? version : VersionComparator.getPreviousVersionForTestcase(testcase, version);
			LOG.debug("Version: {} Predecessor: {}", versionOfPair, predecessor);
			currentPair = new EvaluationPair(versionOfPair, predecessor);
			data.put(versionOfPair, currentPair);
		}

		final Result result = resultData.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0);
		if (!isNew) {
			currentPair.getCurrent().add(result);
		} else {
			currentPair.getPrevius().add(result);
		}

	}

	public SortedMap<String, EvaluationPair> getMeasurements() {
		return data;
	}

	public String getTestClass() {
		return testcase.getClazz();
	}

	public String getTestMethod() {
		return testcase.getMethod();
	}
	
	public int getVersions(){
		return data.size();
	}

}
