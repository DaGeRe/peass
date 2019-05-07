package de.peass.dependency.analysis.data;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.kopeme.generated.Kopemedata.Testcases;

/**
 * Represents a testcase with its class and its method. If no method is given, the whole class with all methods is represented.
 * 
 * @author reichelt
 *
 */
public class TestCase implements Comparable<TestCase>{
   private final String module;
   private final String clazz;
   private final String method;

   public TestCase(final String clazz, final String method) {
      if (clazz.contains(File.separator)) { // possibly assertion, if speed becomes issue..
         throw new RuntimeException("Testcase should be full qualified name, not path: " + clazz);
      }
      this.clazz = clazz;
      this.method = method;
      module = "";
   }

   public TestCase(final String clazz, final String method, final String module) {
      if (clazz.contains(File.separator)) {
         throw new RuntimeException("Testcase " + clazz + " should be full qualified name, not path!");
      }
      this.clazz = clazz;
      this.method = method;
      this.module = module;
   }

   public TestCase(final String testcase) {
      if (testcase.contains(File.separator)) {
         throw new RuntimeException("Testcase should be full qualified name, not path!");
      }
      final int index = testcase.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
      if (index == -1) {
         clazz = testcase;
         method = null;
         // final int indexDot = testcase.lastIndexOf(".");
         // clazz = testcase.substring(0, indexDot);
         // method = testcase.substring(indexDot + 1);
      } else {
         clazz = testcase.substring(0, index);
         method = testcase.substring(index + 1);
      }
      module = null;
   }

   public TestCase(final Testcases testcases) {
      module = "";
      clazz = testcases.getClazz();
      method = testcases.getTestcase().get(0).getName();
   }

   public String getClazz() {
      return clazz;
   }

   public String getMethod() {
      return method;
   }

   public String getModule() {
      return module;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((method == null) ? 0 : method.hashCode());
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
      if (getClass() != obj.getClass()) {
         return false;
      }
      final TestCase other = (TestCase) obj;
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
      if (method == null) {
         if (other.method != null) {
            return false;
         }
      } else if (!method.equals(other.method)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return "TestCase [clazz=" + clazz + ", method=" + method + "]";
   }

   @JsonIgnore
   public String getExecutable() {
      return clazz + "#" + method;
   }

   @JsonIgnore
   public String getShortClazz() {
      return clazz.substring(clazz.lastIndexOf('.') + 1, clazz.length());
   }

   @Override
   public int compareTo(TestCase arg0) {
      return toString().compareTo(arg0.toString());
   }

}