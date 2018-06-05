/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency.analysis.data;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;

/**
 * Represents a set of tests which are executed for one version by its class and its list of methods.
 * 
 * @author reichelt
 *
 */
public class TestSet {
   
   public static class ChangedEntitityDeserializer extends KeyDeserializer {
      
      public ChangedEntitityDeserializer() {
      }
      
      @Override
      public ChangedEntity deserializeKey(String key, DeserializationContext ctxt) throws IOException, JsonProcessingException {
         final String value = key;
         final ChangedEntity entity = new ChangedEntity(value.substring(value.indexOf("-")+1), value.substring(0,value.indexOf("-")));
         
         return entity;
      }
  }
   
   @JsonDeserialize(keyUsing=ChangedEntitityDeserializer.class)
	private final Map<ChangedEntity, List<String>> testcases = new TreeMap<>();

	public TestSet() {

	}

	public TestSet(List<Dependency> dependencies) {
		for (final Dependency dependency : dependencies) {
			for (final Testcase testcase : dependency.getTestcase()) {
				final String clazz = testcase.getClazz();
				for (final String method : testcase.getMethod()) {
					final ChangedEntity entity = new ChangedEntity(clazz, testcase.getModule());
					addTest(entity, method);
				}
			}
		}
	}
	
	public void addTest(final TestCase classname) {
		final ChangedEntity entity = new ChangedEntity(classname.getClazz(), "");
		addTest(entity, classname.getMethod());
	}

	/**
	 * Adds a test to the TestSet. If the method is null, and no further method
	 * is added, the TestSet contains the all methods of the test; if one method
	 * is added, only the method (and perhaps future added methods) are
	 * included.
	 * 
	 * @param classname
	 * @param methodname
	 */
	public void addTest(final ChangedEntity classname, final String methodname) {
		if (classname.getMethod() != null && classname.getMethod() != ""){
			throw new RuntimeException("A testset should only get Changed Entities with empty method");
		}
		List<String> methods = testcases.get(classname);
		if (methods == null) {
			methods = new LinkedList<>();
			testcases.put(classname.copy(), methods);
		}
		if (methodname != null) {
			final String internalizedMethodName = methodname.intern();
			if (!methods.contains(internalizedMethodName)) {
				methods.add(internalizedMethodName);
			}
		}
	}

	public void addTestSet(final TestSet testSet) {
		for (final Map.Entry<ChangedEntity, List<String>> newTestEntry : testSet.entrySet()) {
			List<String> methods = testcases.get(newTestEntry.getKey());
			if (methods == null) {
				methods = new LinkedList<>();
				testcases.put(newTestEntry.getKey().copy(), methods);
			}
			if (newTestEntry.getValue().size() != 0 && methods.size() != 0) {
				methods.addAll(newTestEntry.getValue());
			} else {
				// If List is empty, all methods are changed -> Should be
				// remembered
				methods.clear();
			}
		}
	}

	@JsonIgnore
	public Set<Entry<ChangedEntity, List<String>>> entrySet() {
		return testcases.entrySet();
	}

	@JsonIgnore
	public int size() {
		return testcases.size();
	}

	@JsonIgnore
	public Set<ChangedEntity> getClasses() {
		return testcases.keySet();
	}

	public Map<ChangedEntity, List<String>> getTestcases() {
		return testcases;
	}

	@JsonIgnore
	public void removeTest(final ChangedEntity testClassName, final String testMethodName) {
		if (testClassName.getMethod() != null && testClassName.getMethod() != ""){
			throw new RuntimeException("Testset class removal should only be done with empty method of ChangedEntity!");
		}
		testcases.get(testClassName).remove(testMethodName);
	}

	@Override
	public String toString() {
		return testcases.toString();
	}

	@JsonIgnore
	public List<String> getMethods(String clazz) {
		return testcases.get(clazz);
	}
}
