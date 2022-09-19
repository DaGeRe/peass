package de.dagere.peass.dependency.analysis.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

/**
 * Map from changed classes full-qualified-name to testcases that might have changed
 * @author reichelt
 *
 */
public class ChangeTestMapping {
   
   private static final Logger LOG = LogManager.getLogger(ChangeTestMapping.class);
   
	private final Map<ChangedEntity, Set<TestMethodCall>> changes = new TreeMap<>();

	public Map<ChangedEntity, Set<TestMethodCall>> getChanges() {
		return changes;
	}
	
	public Set<TestMethodCall> getTests(final ChangedEntity change){
	   return changes.get(change);
	}
	
	@Override
	public String toString() {
		return changes.toString();
	}
	
	@JsonIgnore
	public void addChangeEntry(final ChangedEntity changedFullname, final TestMethodCall currentTestcase) {
      Set<TestMethodCall> changedClasses = changes.get(changedFullname);
      if (changedClasses == null) {
         changedClasses = new HashSet<>();
         changes.put(changedFullname, changedClasses);
         // TODO: Statt einfach die Klasse nehmen prüfen, ob die Methode genutzt wird
      }
      LOG.trace("Adding {} to {}", currentTestcase, changedFullname);
      changedClasses.add(currentTestcase);
   }
}
