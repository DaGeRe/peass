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
import com.github.javaparser.ast.type.ArrayType;
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

   private final InstrumentationConfiguration configuration;
   private final BlockBuilder blockBuilder;

   private boolean oneHasChanged = false;

   private int counterIndex = 0;
   private List<String> countersToAdd = new LinkedList<>();
   private List<String> sumsToAdd = new LinkedList<>();

   public FileInstrumenter(File file, InstrumentationConfiguration configuration, BlockBuilder blockBuilder) throws FileNotFoundException {
      this.unit = JavaParserProvider.parse(file);
      this.file = file;
      this.configuration = configuration;
      this.blockBuilder = blockBuilder;
   }

   public void instrument() throws IOException {
      ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
      String packageName = unit.getPackageDeclaration().get().getNameAsString();
      String name = packageName + "." + clazz.getNameAsString();

      boolean fileContainsChange = handleChildren(clazz, name);

      if (fileContainsChange) {
         for (String counterName : countersToAdd) {
            clazz.addField("int", counterName, Keyword.PRIVATE, Keyword.STATIC);
         }
         for (String counterName : sumsToAdd) {
            clazz.addField("long", counterName, Keyword.PRIVATE, Keyword.STATIC);
         }
         addImports(unit);
         Files.write(file.toPath(), unit.toString().getBytes(StandardCharsets.UTF_8));
      }
   }

   private void addImports(CompilationUnit unit) {
      unit.addImport("kieker.monitoring.core.controller.MonitoringController");
      unit.addImport("kieker.monitoring.core.registry.ControlFlowRegistry");
      unit.addImport("kieker.monitoring.core.registry.SessionRegistry");
      unit.addImport(configuration.getUsedRecord().getRecord());
   }

   private boolean handleChildren(ClassOrInterfaceDeclaration clazz, String name) {
      boolean constructorFound = false;
      for (Node child : clazz.getChildNodes()) {
         if (child instanceof MethodDeclaration) {
            instrumentMethod(name, child);
         } else if (child instanceof ConstructorDeclaration) {
            instrumentConstructor(name, child);
            constructorFound = true;
         } else if (child instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration innerClazz = (ClassOrInterfaceDeclaration) child;
            String innerName = innerClazz.getNameAsString();
            handleChildren(innerClazz, name + "$" + innerName);
         }
      }
      if (!constructorFound && configuration.isCreateDefaultConstructor()) {
         String signature = "public new " + name + ".<init>()";
         if (testSignatureMatch(signature)) {
            oneHasChanged = true;
            BlockStmt constructorBlock = blockBuilder.buildEmptyConstructor(signature);
            ConstructorDeclaration constructor = clazz.addConstructor(Modifier.Keyword.PUBLIC);
            constructor.setBody(constructorBlock);
         }
      }
      return oneHasChanged;
   }

   private void instrumentConstructor(String name, Node child) {
      ConstructorDeclaration constructor = (ConstructorDeclaration) child;
      final BlockStmt originalBlock = constructor.getBody();
      SignatureReader reader = new SignatureReader(unit, name);
      String signature = reader.getSignature(constructor);
      boolean oneMatches = testSignatureMatch(signature);
      if (oneMatches) {
         BlockStmt replacedStatement = blockBuilder.buildConstructorStatement(originalBlock, signature, true);

         constructor.setBody(replacedStatement);
         oneHasChanged = true;
      }
   }

   private int instrumentMethod(String name, Node child) {
      MethodDeclaration method = (MethodDeclaration) child;
      final Optional<BlockStmt> body = method.getBody();

      if (body.isPresent()) {
         BlockStmt originalBlock = body.get();
         SignatureReader reader = new SignatureReader(unit, name + "." + method.getNameAsString());
         String signature = reader.getSignature(method);
         boolean oneMatches = testSignatureMatch(signature);
         if (oneMatches) {
            final BlockStmt replacedStatement;
            final boolean needsReturn = method.getType().toString().equals("void");
            if (configuration.isSample()) {
               SamplingParameters parameters = new SamplingParameters(signature, counterIndex);
               countersToAdd.add(parameters.getCounterName());
               sumsToAdd.add(parameters.getSumName());
               replacedStatement = blockBuilder.buildSampleStatement(originalBlock, signature, needsReturn, parameters);
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

   private boolean testSignatureMatch(final String signature) {
      boolean oneMatches = false;
      for (String pattern : configuration.getIncludedPatterns()) {
         pattern = fixConstructorPattern(pattern);
         try {
            System.out.println(pattern);
            Pattern patternP = PatternParser.parseToPattern(pattern);
            if (patternP.matcher(signature).matches()) {
               oneMatches = true;
               break;
            }
         } catch (InvalidPatternException e) {
            LOG.error("Wrong pattern: {}", pattern);
            throw new RuntimeException(e);
         }

      }
      return oneMatches;
   }

   /**
    * In Kieker 1.14, the return type new is ignored for pattern. Therefore, * needs to be set as return type 
    * of constructors in pattern.
    */
   private String fixConstructorPattern(String pattern) {
      if (pattern.contains("<init>")) {
         final String[] tokens = pattern.substring(0, pattern.indexOf('(')).trim().split("\\s+");
         int returnTypeIndex = 0;
         String modifier = "";
         if (tokens[0].equals("private") || tokens[0].equals("public") || tokens[0].equals("protected")) {
            returnTypeIndex++;
            modifier = tokens[0];
         }
         final String returnType = tokens[returnTypeIndex];
         if (returnType.equals("new")) {
            String patternChanged = modifier + " *" + pattern.substring(pattern.indexOf("new") + 3);
            LOG.debug("Changing pattern {} to {}, since Kieker 1.14 does not allow pattern with new", pattern, patternChanged);
            pattern = patternChanged;
         }
      }
      return pattern;
   }

}
