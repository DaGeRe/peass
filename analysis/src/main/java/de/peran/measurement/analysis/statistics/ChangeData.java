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



import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.statistics.PerformanceChange;

/**
 * Saves in which version which performance changes took place and which measurement files influenced this.
 */
public class ChangeData {
	
	private static final Logger LOG = LogManager.getLogger(ChangeData.class);
	
	private final Map<String, Map<PerformanceChange, File>> allChanges;

	public ChangeData() {
		this.allChanges = new LinkedHashMap<>();
	}

	public Map<String, Map<PerformanceChange, File>> getAllChanges() {
		return allChanges;
	}

	/**
	 * Adds a PerformanceChange (containing the version number) which happened and which influenced the given test file.
	 * @param change
	 * @param file
	 */
	public void addChange(final PerformanceChange change, final File file) {
		Map<PerformanceChange, File> revisionMap = allChanges.get(change.getRevision());
		if (revisionMap == null) {
			revisionMap = new HashMap<>();
			allChanges.put(change.getRevision(), revisionMap);
		}
		revisionMap.put(change, file);
	}

	/**
	 * Gets a mapping from class to performance changes, i.e. methods and their measurement deviations, for a given version. Returns an empty map
	 * if no change is known.
	 * 
	 * @param svnCommitId  SVN Commit-Id of the version where the change has taken place
	 * @return mapping from class to performance changes
	 */
	public Map<String, List<PerformanceChange>> getNamePerformancechangeMap(final String svnCommitId) {
		final Map<String, List<PerformanceChange>> changeNames = new HashMap<>();
		final Map<PerformanceChange, File> versionChanges  = allChanges.get(svnCommitId);
		if (versionChanges == null){
			return new HashMap<>();
		}
		for (final PerformanceChange change : versionChanges.keySet()) {
			List<PerformanceChange> changes = changeNames.get(change.getTestClass());
			if (changes == null) {
				changes = new LinkedList<>();
				changeNames.put(change.getTestClass(), changes);
			}
			changes.add(change);
			if (changes.size() > 1) {
				LOG.trace("Mehr: {} . {}" ,change.getTestClass(), change.getTestMethod());
			}
		}
		return changeNames;
	}
}
