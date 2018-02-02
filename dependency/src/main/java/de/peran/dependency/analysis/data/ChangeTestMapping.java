package de.peran.dependency.analysis.data;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Map from changed classes fqn to testcases that might have changed
 * @author reichelt
 *
 */
public class ChangeTestMapping {
	private final Map<String, Set<String>> changes = new TreeMap<>();

	public Map<String, Set<String>> getChanges() {
		return changes;
	}
}
