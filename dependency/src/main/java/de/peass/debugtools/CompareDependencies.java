package de.peass.debugtools;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.InitialDependency;
import de.peass.utils.Constants;
/**
 * Compares two dependency files in order to find whether one is missing some testcases
 * @author reichelt
 *
 */
public class CompareDependencies {
	public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
		final File oldDependenciesFile = new File(args[0]);
		final File newDependenciesFile = new File(args[1]);

      final Dependencies oldDependencies = Constants.OBJECTMAPPER.readValue(oldDependenciesFile, Dependencies.class) ;
		final Dependencies newDependencies = Constants.OBJECTMAPPER.readValue(newDependenciesFile, Dependencies.class);

		int addedCount = 0, missingCount = 0;
		
		final List<ChangedEntity> notFoundNewDependencies = new LinkedList<>();
		notFoundNewDependencies.addAll(newDependencies.getInitialversion().getInitialDependencies().keySet());
		for (final Entry<ChangedEntity, InitialDependency> initialDepOld : oldDependencies.getInitialversion().getInitialDependencies().entrySet()) {
			for (final Entry<ChangedEntity, InitialDependency> initialDepNew : newDependencies.getInitialversion().getInitialDependencies().entrySet()) {
				if (initialDepNew.getKey().equals(initialDepOld.getKey())) {
					final List<String> missing = getDifference(initialDepOld.getValue(), initialDepNew.getValue());

					final List<String> added = getDifference(initialDepNew.getValue(), initialDepOld.getValue());
					if (missing.size() > 0 || added.size() > 0) {
						System.out.println("Test: " + initialDepNew.getKey() + "(" + initialDepNew.getValue().getEntities().size() 
						      +" " + initialDepOld.getValue().getEntities().size() + ")");
						System.out.println("Missing: " + missing);
						System.out.println("Added: " + added);
						
						addedCount+=added.size();
						missingCount+=missing.size();
					}

					notFoundNewDependencies.remove(initialDepNew.getKey());
				}
			}
		}
		System.out.println("Added: " + addedCount + " Missing: " + missingCount);

		System.out.println("Missing testcases: " + notFoundNewDependencies.size());

		for (final ChangedEntity change : notFoundNewDependencies) {
			System.out.println("Missing: " + change.toString());
		}

	}

	private static List<String> getDifference(final InitialDependency initialDepOld, final InitialDependency initialDepNew) {
		final List<String> missing = new LinkedList<>();
		for (final ChangedEntity clazz : initialDepOld.getEntities()){
			missing.add(clazz.toString());
		}
		for (final ChangedEntity clazz : initialDepNew.getEntities()){
			missing.remove(clazz.toString());
		}
		for (final Iterator<String> it = missing.iterator(); it.hasNext();) {
			final String current = it.next();
			if (current.endsWith("getMaximalTime") || current.endsWith("getExecutionTimes") || current.endsWith("getWarmupExecutions")) {
				it.remove();
			}
		}
		return missing;
	}
}
