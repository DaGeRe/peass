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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import de.peran.DependencyStatisticAnalyzer;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Initialversion.Initialdependency;
/**
 * Compares two dependency files in order to find whether one is missing some testcases
 * @author reichelt
 *
 */
public class CompareDependencies {
	public static void main(String[] args) throws JAXBException {
		final File oldDependenciesFile = new File(args[0]);
		final File newDependenciesFile = new File(args[1]);

		final Versiondependencies oldDependencies = DependencyStatisticAnalyzer.readVersions(oldDependenciesFile);
		final Versiondependencies newDependencies = DependencyStatisticAnalyzer.readVersions(newDependenciesFile);

		int addedCount = 0, missingCount = 0;
		
		final List<Initialdependency> notFoundNewDependencies = new LinkedList<>();
		notFoundNewDependencies.addAll(newDependencies.getInitialversion().getInitialdependency());
		for (Initialdependency initialDepOld : oldDependencies.getInitialversion().getInitialdependency()) {
			for (Initialdependency initialDepNew : newDependencies.getInitialversion().getInitialdependency()) {
				if (initialDepNew.getTestclass().equals(initialDepOld.getTestclass())) {
					List<String> missing = getDifference(initialDepOld, initialDepNew);

					List<String> added = getDifference(initialDepNew, initialDepOld);
					if (missing.size() > 0 || added.size() > 0) {
						System.out.println("Test: " + initialDepNew.getTestclass() + "(" + initialDepNew.getDependentclass().size() +" " + initialDepOld.getDependentclass().size() + ")");
						System.out.println("Missing: " + missing);
						System.out.println("Added: " + added);
						
						addedCount+=added.size();
						missingCount+=missing.size();
					}

					notFoundNewDependencies.remove(initialDepNew);
				}
			}
		}
		System.out.println("Added: " + addedCount + " Missing: " + missingCount);

		System.out.println("Missing testcases: " + notFoundNewDependencies.size());

		for (Initialdependency change : notFoundNewDependencies) {
			System.out.println("Missing: " + change.getTestclass());
		}

	}

	private static List<String> getDifference(final Initialdependency initialDepOld, final Initialdependency initialDepNew) {
		List<String> missing = new LinkedList<>();
		missing.addAll(initialDepOld.getDependentclass());
		missing.removeAll(initialDepNew.getDependentclass());
		for (Iterator<String> it = missing.iterator(); it.hasNext();) {
			String current = it.next();
			if (current.endsWith("getMaximalTime") || current.endsWith("getExecutionTimes") || current.endsWith("getWarmupExecutions")) {
				it.remove();
			}
		}
		return missing;
	}
}
