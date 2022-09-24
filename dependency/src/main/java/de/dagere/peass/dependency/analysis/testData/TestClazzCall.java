package de.dagere.peass.dependency.analysis.testData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestClazzCall extends TestCase {
   private static final long serialVersionUID = 7326687309901903065L;

   public TestClazzCall(String clazz) {
      super(clazz, "");
   }

   public TestClazzCall(@JsonProperty("clazz")  String clazz, 
         @JsonProperty("module") String module) {
      super(clazz, module);
   }
   
   public TestClazzCall copy() {
      TestClazzCall test = new TestClazzCall(clazz, module);
      return test;
   }

   public ChangedEntity toEntity() {
      return new ChangedEntity(clazz, module);
   }
   
   @JsonIgnore
   public String getExecutable() {
      return clazz;
   }
   
   public static TestClazzCall createFromString(String testcase) {
      String module, clazz;
      int moduleIndex = testcase.indexOf(ChangedEntity.MODULE_SEPARATOR);
      if (moduleIndex == -1) {
         clazz = testcase;
         module = "";
      } else {
         clazz = testcase.substring(moduleIndex + 1);
         module = testcase.substring(0, moduleIndex);
      }
      return new TestClazzCall(clazz, module);
   }
   
   @Override
   public String toString() {
      String result;
      if (module != null && !"".equals(module)) {
         result = module + ChangedEntity.MODULE_SEPARATOR + clazz;
      } else {
         result = clazz;
      }
      return result;
   }
   
   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      final TestClazzCall other = (TestClazzCall) obj;
      if (clazz == null) {
         if (other.clazz != null) {
            return false;
         }
      } else if (!clazz.equals(other.clazz)) {
         final String shortClazz = clazz.substring(clazz.lastIndexOf('.') + 1);
         final String shortClazzOther = other.getClazz().substring(other.getClazz().lastIndexOf('.') + 1);
         if (!shortClazz.equals(shortClazzOther)) { // Dirty Hack - better transfer clazz-info always
            return false;
         }
      }
      return true;
   }
}
