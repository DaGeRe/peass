package de.peass.kiekerInstrument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;

import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.execution.AllowedKiekerRecord;
import de.peass.testtransformation.ParseUtil;
import kieker.monitoring.core.signaturePattern.InvalidPatternException;
import kieker.monitoring.core.signaturePattern.PatternParser;

public class FileInstrumenter {
   
   private static final Logger LOG = LogManager.getLogger(FileInstrumenter.class);
   
   private final CompilationUnit unit;
   private final File file;
   
   private final AllowedKiekerRecord usedRecord;
   private final Set<String> includedPatterns;
   private final BlockBuilder blockBuilder;
   private final boolean sample;
   
   boolean oneHasChanged = false;
   
   public FileInstrumenter(File file, AllowedKiekerRecord usedRecord, Set<String> includedPatterns, BlockBuilder blockBuilder, boolean sample) throws FileNotFoundException  {
      this.unit = JavaParserProvider.parse(file);
      this.file = file;
      this.usedRecord = usedRecord;
      this.includedPatterns = includedPatterns;
      this.blockBuilder = blockBuilder;
      this.sample = sample;
   }
   
   public void instrument() throws IOException {
      ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
      String packageName = unit.getPackageDeclaration().get().getNameAsString();
      String name = packageName + "." + clazz.getNameAsString();

      boolean fileContainsChange = handleChildren(clazz, name);

      if (fileContainsChange) {
         addImports(unit);
         Files.write(file.toPath(), unit.toString().getBytes(StandardCharsets.UTF_8));
      }
   }
   
   private void addImports(CompilationUnit unit) {
      unit.addImport("kieker.monitoring.core.controller.MonitoringController");
      unit.addImport("kieker.monitoring.core.registry.ControlFlowRegistry");
      unit.addImport("kieker.monitoring.core.registry.SessionRegistry");
      unit.addImport(usedRecord.getRecord());
   }

   private boolean handleChildren(ClassOrInterfaceDeclaration clazz, String name) {
      int counterIndex = 0;
      List<String> countersToAdd = new LinkedList<>();
      List<String> sumsToAdd = new LinkedList<>();
      for (Node child : clazz.getChildNodes()) {
         if (child instanceof MethodDeclaration) {
            counterIndex = instrumentMethod(name, counterIndex, countersToAdd, sumsToAdd, child);
         } else if (child instanceof ConstructorDeclaration) {
            instrumentConstructor(name, child);
         } else if (child instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration innerClazz = (ClassOrInterfaceDeclaration) child;
            String innerName = innerClazz.getNameAsString();
            handleChildren(innerClazz, name + "$" + innerName);
         }
      }
      for (String counterName : countersToAdd) {
         clazz.addField("int", counterName, Keyword.PRIVATE, Keyword.STATIC);
      }
      for (String counterName : sumsToAdd) {
         clazz.addField("long", counterName, Keyword.PRIVATE, Keyword.STATIC);
      }
      return oneHasChanged;
   }

   private void instrumentConstructor(String name, Node child) {
      ConstructorDeclaration constructor = (ConstructorDeclaration) child;
      final BlockStmt originalBlock = constructor.getBody();
      String signature = getSignature(name, constructor);
      boolean oneMatches = testSignatureMatch(signature);
      if (oneMatches) {
         BlockStmt replacedStatement = blockBuilder.buildConstructorStatement(originalBlock, signature, true);

         constructor.setBody(replacedStatement);
         oneHasChanged = true;
      }
   }

   private int instrumentMethod(String name, int counterIndex, List<String> countersToAdd, List<String> sumsToAdd, Node child) {
      MethodDeclaration method = (MethodDeclaration) child;
      final Optional<BlockStmt> body = method.getBody();

      if (body.isPresent()) {
         BlockStmt originalBlock = body.get();
         String signature = getSignature(name + "." + method.getNameAsString(), method);
         boolean oneMatches = testSignatureMatch(signature);
         if (oneMatches) {
            final BlockStmt replacedStatement;
            final boolean needsReturn = method.getType().toString().equals("void");
            if (sample) {
               final String nameBeforeParanthesis = signature.substring(0, signature.indexOf('('));
               final String methodNameSubstring = nameBeforeParanthesis.substring(nameBeforeParanthesis.lastIndexOf('.') + 1);
               final String counterName = methodNameSubstring + "Counter" + counterIndex;
               final String sumName = methodNameSubstring + "Sum" + counterIndex;
               countersToAdd.add(counterName);
               sumsToAdd.add(sumName);
               replacedStatement = blockBuilder.buildSampleStatement(originalBlock, signature, needsReturn, counterName, sumName);
               counterIndex++;
            } else {
               replacedStatement = blockBuilder.buildStatement(originalBlock, signature, needsReturn);
            }

            method.setBody(replacedStatement);
            oneHasChanged = true;
         }
      } else {
         LOG.info("Unable to instrument " + name + "." + method.getNameAsString() + " because it has no body");
      }
      return counterIndex;
   }

   private boolean testSignatureMatch(String signature) {
      boolean oneMatches = false;
      for (String pattern : includedPatterns) {
         try {
            Pattern patternP = PatternParser.parseToPattern(pattern);
            if (patternP.matcher(signature).matches()) {
               oneMatches = true;
               break;
            }
         } catch (InvalidPatternException e) {
            throw new RuntimeException(e);
         }

      }
      return oneMatches;
   }

   private String getSignature(String name, MethodDeclaration method) {
      String modifiers = getModifierString(method.getModifiers());
      String returnTypFQN = getTypeFQN(method.getType(), method.getTypeAsString());
      final String returnType = returnTypFQN + " ";
      String signature = modifiers + returnType + name + "(";
      signature += getParameterString(method);
      signature += ")";
      return signature;
   }

   private String getParameterString(MethodDeclaration method) {
      String parameterString = "";
      for (Parameter parameter : method.getParameters()) {
         final String parameterName = parameter.getType().asString();
         String fqn = getTypeFQN(parameter.getType(), parameterName);
         parameterString += fqn + ",";
      }
      if (parameterString.length() > 0) {
         parameterString = parameterString.substring(0, parameterString.length() - 1);
      }
      return parameterString;
   }

   private String getTypeFQN(Type type, final String parameterName) {
      if (parameterName.equals("void")) {
         return parameterName;
      }
      String fqn;
      if (type.isPrimitiveType()) {
         fqn = parameterName;
      } else {
         ImportDeclaration currentImport = null;
         for (ImportDeclaration importDeclaration : unit.getImports()) {
            final String importFqn = importDeclaration.getNameAsString();
            if (importFqn.endsWith("." + parameterName)) {
               currentImport = importDeclaration;
               break;
            }
         }
         if (currentImport != null) {
            fqn = currentImport.getNameAsString();
         } else {
            fqn = "java.lang." + parameterName;
         }
      }
      return fqn;
   }

   private String getSignature(String name, ConstructorDeclaration method) {
      String modifiers = getModifierString(method.getModifiers());
      String signature = modifiers + "new " + name + ".<init>(" + ")";
      return signature;
   }

   private String getModifierString(NodeList<Modifier> listOfModifiers) {
      String modifiers = "";
      for (Modifier modifier : listOfModifiers) {
         modifiers += modifier;
      }
      return modifiers;
   }
}
