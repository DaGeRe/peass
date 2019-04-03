package de.peass.dependency.analysis.data;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Map from changed classes full-qualified-name to testcases that might have changed
 * @author reichelt
 *
 */
public class ChangeTestMapping {
	private final Map<ChangedEntity, Set<ChangedEntity>> changes = new TreeMap<>();

	public Map<ChangedEntity, Set<ChangedEntity>> getChanges() {
		return changes;
	}
	
	public Set<ChangedEntity> getTests(ChangedEntity change){
	   return changes.get(change);
	}
	
	@Override
	public String toString() {
		return changes.toString();
	}
}
