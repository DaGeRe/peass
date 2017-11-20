package de.peran.measurement.analysis.statistics;

import java.util.LinkedList;
import java.util.List;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;

/**
 * Represents an pair of measurement results that should be evaluated, i.e. the versions of 
 * both measurements and its results.
 * @author reichelt
 *
 */
public class EvaluationPair {
 
	private final String previousVersion, currentVersion;
	private final List<Result> previus = new LinkedList<>();
	private final List<Result> current = new LinkedList<>();

	public EvaluationPair(final String currentVersion, final String previousVersion) {
		this.currentVersion = currentVersion;
		this.previousVersion = previousVersion;
		if (currentVersion.equals(previousVersion)) {
			throw new RuntimeException("Unexpected behaviour: Previous " + previousVersion + " == Current " + currentVersion + " version.");
		}
		if (currentVersion == null || previousVersion == null){
			throw new RuntimeException("Version == null: " + currentVersion + " " + previousVersion);
		}
	}

	public List<Result> getPrevius() {
		return previus;
	}

	public List<Result> getCurrent() {
		return current;
	}

	public boolean isComplete() {
		boolean isComplete = previus.size() > 0 && previus.size() == current.size();
		if (isComplete){
			isComplete &= previus.get(0).getFulldata() != null;
			isComplete &= current.get(0).getFulldata() != null;
			if (isComplete){
				isComplete &= previus.get(0).getFulldata().getValue().size() > 0;
				isComplete &= current.get(0).getFulldata().getValue().size() > 0;
			}
		}
		return isComplete;
	}

	public String getPreviusVersion() {
		return previousVersion;
	}

	public String getVersion() {
		return currentVersion;
	}
}