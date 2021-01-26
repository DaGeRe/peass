package de.peass.kiekerInstrument;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;

import de.peass.dependency.changesreading.ClazzFinder;

import de.peass.dependency.ClazzFileFinder;

public class SignatureReader {

   /**
    * These contain all classes from java.lang, obtained from http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/lang/, saved to ~/test.csv and
    * processed by cat ~/test.csv | awk '{print $2}' | awk -F'.' '{print $1}' | tr "\n" " " | sed "s/ /\",\"/g" While it would be nicer to get them by reflections (so the list
    * would be up-to-date for the current JVM), I did not see a way to do so
    */
   private static final List<String> javaLangClasses = new LinkedList<>(Arrays.asList(new String[] {
         "AbstractMethodError", "AbstractStringBuilder", "Appendable", "ApplicationShutdownHooks", "ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException",
         "AssertionError", "AssertionStatusDirectives", "AutoCloseable", "Boolean", "BootstrapMethodError", "Byte", "CharSequence", "Character", "CharacterData", "CharacterName",
         "Class", "ClassCastException", "ClassCircularityError", "ClassFormatError", "ClassLoader", "ClassNotFoundException", "ClassValue", "CloneNotSupportedException",
         "Cloneable", "Comparable", "Compiler", "ConditionalSpecialCasing", "Deprecated", "Double", "Enum", "EnumConstantNotPresentException", "Error", "Exception",
         "ExceptionInInitializerError", "Float", "FunctionalInterface", "IllegalAccessError", "IllegalAccessException", "IllegalArgumentException", "IllegalMonitorStateException",
         "IllegalStateException", "IllegalThreadStateException", "IncompatibleClassChangeError", "IndexOutOfBoundsException", "InheritableThreadLocal", "InstantiationError",
         "InstantiationException", "Integer", "InternalError", "InterruptedException", "Iterable", "LinkageError", "Long", "Math", "NegativeArraySizeException",
         "NoClassDefFoundError", "NoSuchFieldError", "NoSuchFieldException", "NoSuchMethodError", "NoSuchMethodException", "NullPointerException", "Number",
         "NumberFormatException", "Object", "OutOfMemoryError", "Override", "Package", "Process", "ProcessBuilder", "Readable", "ReflectiveOperationException", "Runnable",
         "Runtime", "RuntimeException", "RuntimePermission", "SafeVarargs", "SecurityException", "SecurityManager", "Short", "Shutdown", "StackOverflowError", "StackTraceElement",
         "StrictMath", "String", "StringBuffer", "StringBuilder", "StringCoding", "StringIndexOutOfBoundsException", "SuppressWarnings", "System", "Thread", "ThreadDeath",
         "ThreadGroup", "ThreadLocal", "Throwable", "TypeNotPresentException", "UnknownError", "UnsatisfiedLinkError", "UnsupportedClassVersionError",
         "UnsupportedOperationException", "VerifyError", "VirtualMachineError", "Void"
   }));

   private final CompilationUnit unit;
   private final String name;
   private final List<String> localClazzes;

   public SignatureReader(CompilationUnit unit, String name) {
      this.unit = unit;
      this.name = name;
      localClazzes = ClazzFinder.getClazzes(unit);
   }

   public String getSignature(MethodDeclaration method) {
      String modifiers = getModifierString(method.getModifiers());
      String returnTypFQN = getTypeFQN(method.getType());
      final String returnType = returnTypFQN + " ";
      String signature = modifiers + returnType + name + "(";
      signature += getParameterString(method.getParameters());
      signature += ")";
      return signature;
   }

   public String getSignature(ConstructorDeclaration method) {
      String modifiers = getModifierString(method.getModifiers());
      String signature = modifiers + "new " + name + ".<init>(";
      if (name.contains("$")) {
         String firstConstructorPart = name.substring(0, name.indexOf('$'));
         signature += firstConstructorPart + ",";
      }
      signature += getParameterString(method.getParameters());
      signature += ")";
      return signature;
   }

   private String getParameterString(NodeList<Parameter> methodParameters) {
      String parameterString = "";
      for (Parameter parameter : methodParameters) {
         String fqn = getTypeFQN(parameter.getType());
         parameterString += fqn + ",";
      }
      if (parameterString.length() > 0) {
         parameterString = parameterString.substring(0, parameterString.length() - 1);
      }
      return parameterString;
   }

   private String getTypeFQN(Type type) {
      String typeName = type.asString();
      if (typeName.equals("void")) {
         return typeName;
      }
      String fqn;
      if (type.isPrimitiveType()) {
         fqn = typeName;
      } else if (type.isArrayType()) {
         ArrayType arrayType = (ArrayType) type;
         Type componentType = arrayType.getComponentType();
         String arrayString = arrayType.asString().substring(arrayType.asString().indexOf('['));
         if (componentType.isPrimitiveType()) {
            fqn = typeName;
         } else {
            String typeNameWithoutArray = typeName.substring(0, typeName.indexOf('['));
            ImportDeclaration currentImport = findImport(typeNameWithoutArray);
            if (currentImport != null) {
               fqn = currentImport.getNameAsString() + arrayString;
            } else {
               fqn = getReferenceInnerClazz(typeNameWithoutArray);
            }
         }
         if (!fqn.endsWith(arrayString)) {
            fqn = fqn + arrayString;
         }
      } else {
         ImportDeclaration currentImport = findImport(typeName);
         if (currentImport != null) {
            fqn = currentImport.getNameAsString();
         } else {
            fqn = getReferenceInnerClazz(typeName);
         }
      }
      return fqn;
   }

   private String getReferenceInnerClazz(String typeNameWithoutArray) {
      String fqn;
      // This does not work if a inner class is declared that matches the outer class, e.g. C0_0.C0_0
      String matchingInnerClass = null;
      for (String innerClass : localClazzes) {
         if (innerClass.endsWith(typeNameWithoutArray)) {
            matchingInnerClass = innerClass;
         }
      }
      if (matchingInnerClass != null) {
         fqn = unit.getPackageDeclaration().get().getNameAsString() + "." + matchingInnerClass;
      } else if (javaLangClasses.contains(typeNameWithoutArray)) {
         fqn = "java.lang." + typeNameWithoutArray;
      } else {
         fqn = unit.getPackageDeclaration().get().getNameAsString() + "." + typeNameWithoutArray;
      }
      return fqn;
   }

   private ImportDeclaration findImport(String typeName) {
      ImportDeclaration currentImport = null;
      for (ImportDeclaration importDeclaration : unit.getImports()) {
         final String importFqn = importDeclaration.getNameAsString();
         if (importFqn.endsWith("." + typeName)) {
            currentImport = importDeclaration;
            break;
         }
      }
      return currentImport;
   }

   private String getModifierString(NodeList<Modifier> listOfModifiers) {
      String modifiers = "";
      for (Modifier modifier : listOfModifiers) {
         modifiers += modifier;
      }
      return modifiers;
   }
}
