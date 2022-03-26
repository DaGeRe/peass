package de.dagere.peass.dependency.changesreading;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.dagere.peass.dependency.traces.TraceReadUtils;

public class FQNDeterminer {

   private static final Logger LOG = LogManager.getLogger(FQNDeterminer.class);

   // TODO This are only the Java 8 classes; while it is not very likely that people heavily rely on things like System.Logger as method parameters, it would be better to include
   // them here (but only for builds using Java 11)
   private static final String[] JAVA_LANG_CLASSES = new String[] { "Appendable", "AutoCloseable", "CharSequence", "Cloneable", "Comparable<T>", "Iterable<T>", "Readable",
         "Runnable", "Thread.UncaughtExceptionHandler", "Boolean", "Byte", "Character", "Character.Subset", "Character.UnicodeBlock", "Class<T>", "ClassLoader", "ClassValue<T>",
         "Compiler", "Double", "Enum<E", "Float", "InheritableThreadLocal<T>", "Integer", "Long", "Math", "Number", "Object", "Package", "Process", "ProcessBuilder",
         "ProcessBuilder.Redirect", "Runtime", "RuntimePermission", "SecurityManager", "Short", "StackTraceElement", "StrictMath", "String", "StringBuffer", "StringBuilder",
         "System", "Thread", "ThreadGroup", "ThreadLocal<T>", "Throwable", "Void", "", "Character.UnicodeScript", "ProcessBuilder.Redirect.Type", "Thread.State", "",
         "ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException", "ClassCastException", "ClassNotFoundException", "CloneNotSupportedException",
         "EnumConstantNotPresentException", "Exception", "IllegalAccessException", "IllegalArgumentException", "IllegalMonitorStateException", "IllegalStateException",
         "IllegalThreadStateException", "IndexOutOfBoundsException", "InstantiationException", "InterruptedException", "NegativeArraySizeException", "NoSuchFieldException",
         "NoSuchMethodException", "NullPointerException", "NumberFormatException", "ReflectiveOperationException", "RuntimeException", "SecurityException",
         "StringIndexOutOfBoundsException", "TypeNotPresentException", "UnsupportedOperationException", "AbstractMethodError", "AssertionError", "BootstrapMethodError",
         "ClassCircularityError", "ClassFormatError", "Error", "ExceptionInInitializerError", "IllegalAccessError", "IncompatibleClassChangeError", "InstantiationError",
         "InternalError", "LinkageError", "NoClassDefFoundError", "NoSuchFieldError", "NoSuchMethodError", "OutOfMemoryError", "StackOverflowError", "ThreadDeath", "UnknownError",
         "UnsatisfiedLinkError", "UnsupportedClassVersionError", "VerifyError", "VirtualMachineError", "FunctionalInterface", "Deprecated", "Override", "SafeVarargs",
         "SuppressWarnings" };
   private static final Set<String> JAVA_LANG_CLASSES_SET = new HashSet<>(Arrays.asList(JAVA_LANG_CLASSES));

   private static final String[] PRIMITIVE_NAMES = new String[]{"boolean","byte","char","short","int","long","float","double"};
   private static final Set<String> PRIMITIVE_NAMES_SET = new HashSet<>(Arrays.asList(PRIMITIVE_NAMES));

   public static String getParameterFQN(final CompilationUnit unit, final String typeName) {
      if (PRIMITIVE_NAMES_SET.contains(typeName)) {
         return typeName;
      }
      
      String localDeclaration = checkLocallyDeclaredType(unit, typeName);
      if (localDeclaration != null) {
         return localDeclaration;
      }

      String importedType = checkImportedType(unit, typeName);
      if (importedType != null) {
         return importedType;
      }

      if (JAVA_LANG_CLASSES_SET.contains(typeName) || JAVA_LANG_CLASSES_SET.contains(typeName + "<T>")) {
         return "java.lang." + typeName;
      } else {
         String packageName = unit.getPackageDeclaration().get().getNameAsString();
         String typeFQN = packageName + "." + typeName;
         return typeFQN;
      }
   }

   private static String checkImportedType(final CompilationUnit unit, final String typeName) {
      String importedType = null;
      for (ImportDeclaration importDecl : unit.getImports()) {
         if (importDecl.isAsterisk()) {
            // TODO This will not work with wildcard declarations
            LOG.error("Wildcard declaration {} is likely to not be parsed correctly; FQN from monitoring and FQN from parsing may differ", importDecl);
         }
         if (importDecl.getNameAsString().endsWith("." + typeName) || importDecl.getNameAsString().equals(typeName)) {
            importedType = importDecl.getNameAsString();
         }
      }
      return importedType;
   }

   private static String checkLocallyDeclaredType(final CompilationUnit unit, final String typeName) {
      Map<String, TypeDeclaration<?>> declarations = TraceReadUtils.getNamedClasses(unit, "");
      String localDeclaration = null;
      for (Map.Entry<String, TypeDeclaration<?>> declaration : declarations.entrySet()) {
         if (declaration.getKey().endsWith("$" + typeName) || declaration.getKey().equals(typeName)) {
            String packageName = unit.getPackageDeclaration().get().getNameAsString();
            String typeFQN = packageName + "." + declaration.getKey();
            localDeclaration = typeFQN;

         }
      }
      return localDeclaration;
   }
}
