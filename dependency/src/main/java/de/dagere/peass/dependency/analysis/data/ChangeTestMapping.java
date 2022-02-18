package de.dagere.peass.dependency.analysis.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Map from changed classes full-qualified-name to testcases that might have changed
 * @author reichelt
 *
 */
public class ChangeTestMapping {
   
   private static final Logger LOG = LogManager.getLogger(ChangeTestMapping.class);
   
	private final Map<ChangedEntity, Set<TestCase>> changes = new TreeMap<>();

	public Map<ChangedEntity, Set<TestCase>> getChanges() {
		return changes;
	}
	
	public Set<TestCase> getTests(final ChangedEntity change){
	   return changes.get(change);
	}
	
	@Override
	public String toString() {
		return changes.toString();
	}
	
	@JsonIgnore
	public void addChangeEntry(final ChangedEntity changedFullname, final TestCase currentTestcase) {
      Set<TestCase> changedClasses = changes.get(changedFullname);
      if (changedClasses == null) {
         changedClasses = new HashSet<>();
         changes.put(changedFullname, changedClasses);
         // TODO: Statt einfach die Klasse nehmen prüfen, ob die Methode genutzt wird
      }
      LOG.debug("Füge {} zu {} hinzu", currentTestcase, changedFullname);
      changedClasses.add(currentTestcase);
   }
}
