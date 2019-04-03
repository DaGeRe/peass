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
package de.peass.dependency.analysis.data;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jline.internal.Log;

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
      public ChangedEntity deserializeKey(final String key, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
         String value = key;
         final ChangedEntity entity;

         String method = "";
         if (value.contains(ChangedEntity.METHOD_SEPARATOR)) {
            method = value.substring(value.indexOf(ChangedEntity.METHOD_SEPARATOR) + 1);
            value = value.substring(0, value.indexOf(ChangedEntity.METHOD_SEPARATOR));
         }

         if (value.contains(ChangedEntity.MODULE_SEPARATOR)) {
            final String clazz = value.substring(value.indexOf(ChangedEntity.MODULE_SEPARATOR) + 1);
            final String module = value.substring(0, value.indexOf(ChangedEntity.MODULE_SEPARATOR));
            entity = new ChangedEntity(clazz, module, method);
         } else {
            entity = new ChangedEntity(value, "", method);
         }

         return entity;
      }
   }

   @JsonDeserialize(keyUsing = ChangedEntitityDeserializer.class, contentAs = TreeSet.class)
   private final Map<ChangedEntity, Set<String>> testcases = new TreeMap<>();
   private String predecessor;

   public TestSet() {
   }

   public TestSet(final String testcase) {
      addTest(new TestCase(testcase));
   }

   @JsonIgnore
   public void addTest(final TestCase classname) {
      final ChangedEntity entity = new ChangedEntity(classname.getClazz(), classname.getModule());
      addTest(entity, classname.getMethod());
   }

   /**
    * Adds a test to the TestSet. If the method is null, and no further method is added, the TestSet contains the all methods of the test; if one method is added, only the method
    * (and perhaps future added methods) are included.
    * 
    * @param classname
    * @param methodname
    */
   public void addTest(final ChangedEntity classname, final String methodname) {
      if (classname.getMethod() != null && classname.getMethod() != "") {
         throw new RuntimeException("A testset should only get Changed Entities with empty method");
      }
      Set<String> methods = testcases.get(classname);
      if (methods == null) {
         methods = new TreeSet<>();
         testcases.put(classname.copy(), methods);
      }
      if (methodname != null) {
         final String internalizedMethodName = methodname.intern();
         if (!methods.contains(internalizedMethodName)) {
            methods.add(internalizedMethodName);
         }
      }
   }

   @JsonIgnore
   public void addTestSet(final TestSet testSet) {
      for (final Map.Entry<ChangedEntity, Set<String>> newTestEntry : testSet.entrySet()) {
         Set<String> methods = testcases.get(newTestEntry.getKey());
         if (methods == null) {
            methods = new TreeSet<>();
            methods.addAll(newTestEntry.getValue());
            testcases.put(newTestEntry.getKey().copy(), methods);
         } else {
            if (newTestEntry.getValue().size() != 0 && methods.size() != 0) {
               methods.addAll(newTestEntry.getValue());
            } else {
               // If List is empty, all methods are changed -> Should be
               // remembered
               methods.clear();
            }
         }
      }
   }

   @JsonIgnore
   public Set<Entry<ChangedEntity, Set<String>>> entrySet() {
      return testcases.entrySet();
   }

   @JsonIgnore
   public int classCount() {
      return testcases.size();
   }

   @JsonIgnore
   public Set<ChangedEntity> getClasses() {
      return testcases.keySet();
   }

   @JsonIgnore
   public Set<TestCase> getTests() {
      final Set<TestCase> testcases = new HashSet<>();
      for (final Entry<ChangedEntity, Set<String>> classTests : getTestcases().entrySet()) {
         if (classTests.getValue().size() > 0) {
            for (final String method : classTests.getValue()) {
               final TestCase testcase = new TestCase(classTests.getKey().getClazz(), method, classTests.getKey().getModule());
               testcases.add(testcase);
            }
         } else {
            testcases.add(new TestCase(classTests.getKey().getClazz(), "", classTests.getKey().getModule()));
         }
      }
      return testcases;
   }

   public Map<ChangedEntity, Set<String>> getTestcases() {
      return testcases;
   }

   public void removeTest(final ChangedEntity testClassName) {
      if (testClassName.getMethod() != null && testClassName.getMethod() != "") {
         throw new RuntimeException("Testset class removal should only be done with empty method of ChangedEntity!");
      }
      testcases.remove(testClassName);
   }

   @JsonIgnore
   public void removeTest(final ChangedEntity testClassName, final String testMethodName) {
      if (testClassName.getMethod() != null && testClassName.getMethod() != "") {
         throw new RuntimeException("Testset class removal should only be done with empty method of ChangedEntity!");
      }
      final Set<String> testMethods = testcases.get(testClassName);
      if (testMethods != null) {
         System.out.println("Remove: " + testClassName + "#" + testMethodName);
         if (!testMethods.remove(testMethodName)) {
            System.out.println("Problem: " + testMethodName + " can not be removed.");
            // throw new RuntimeException("Test " + testMethodName + " was not in TestSet!");
         }
         if (testMethods.size() == 0) {
            testcases.remove(testClassName);
         }
      } else {
         Log.error("Testclass " + testClassName + " missing");
      }
   }

   @Override
   public String toString() {
      return testcases.toString();
   }

   @JsonIgnore
   public Set<String> getMethods(final ChangedEntity clazz) {
      return testcases.get(clazz);
   }

   @JsonInclude(Include.NON_EMPTY)
   public String getPredecessor() {
      return predecessor;
   }

   public void setPredecessor(final String predecessor) {
      this.predecessor = predecessor;
   }
}
