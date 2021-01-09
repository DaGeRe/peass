package de.peass.kiekerInstrument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.stmt.BlockStatement;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.PrimitiveType;

import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.execution.AllowedKiekerRecord;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.testtransformation.ParseUtil;
import javassist.compiler.ast.MethodDecl;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.registry.ControlFlowRegistry;
import kieker.monitoring.core.registry.SessionRegistry;
import kieker.monitoring.core.signaturePattern.InvalidPatternException;
import kieker.monitoring.core.signaturePattern.PatternParser;

/**
 * Adds kieker monitoring code to existing source code *in-place*, i.e. the existing .java-files will get changed.
 * 
 * @author reichelt
 *
 */
public class InstrumentKiekerSource {

   private static final Logger LOG = LogManager.getLogger(InstrumentKiekerSource.class);

   private final AllowedKiekerRecord usedRecord;
   private final Set<String> includedPatterns;
   private final BlockBuilder blockBuilder;
   private final boolean sample;

   public InstrumentKiekerSource(AllowedKiekerRecord usedRecord) {
      this.usedRecord = usedRecord;
      includedPatterns = new HashSet<>();
      includedPatterns.add("*");
      this.blockBuilder = new BlockBuilder(usedRecord, true);
      this.sample = false;
   }

   public InstrumentKiekerSource(AllowedKiekerRecord usedRecord, Set<String> includedPatterns, boolean sample) {
      this.usedRecord = usedRecord;
      this.includedPatterns = includedPatterns;
      this.blockBuilder = new BlockBuilder(usedRecord, false);
      this.sample = sample;
   }

   public void instrumentProject(File projectFolder) throws IOException {
      for (File javaFile : FileUtils.listFiles(projectFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
         LOG.trace("Instrumenting: " + javaFile);
         instrument(javaFile);
      }
   }

   public void instrument(File file) throws IOException {
      CompilationUnit unit = JavaParserProvider.parse(file); // TODO Package

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
      boolean oneHasChanged = false;
      List<String> countersToAdd = new LinkedList<>();
      List<String> sumsToAdd = new LinkedList<>();
      for (Node child : clazz.getChildNodes()) {
         if (child instanceof MethodDeclaration) {
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
                     String counterName = signature.substring(signature.lastIndexOf('.') + 1, signature.indexOf('(')) + "Counter" + counterIndex;
                     String sumName = signature.substring(signature.lastIndexOf('.') + 1, signature.indexOf('(')) + "Sum" + counterIndex;
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
         } else if (child instanceof ConstructorDeclaration) {
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
      }
      for (String counterName : countersToAdd) {
         clazz.addField("int", counterName, Keyword.PRIVATE);
      }
      for (String counterName : sumsToAdd) {
         clazz.addField("long", counterName, Keyword.PRIVATE);
      }
      return oneHasChanged;
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
      String modifiers = "";
      for (Modifier modifier : method.getModifiers()) {
         modifiers += modifier;
      }
      String returnType = method.getType().toString() + " ";
      String signature = modifiers + returnType + name + "(" + ")";
      return signature;
   }

   private String getSignature(String name, ConstructorDeclaration method) {
      String modifiers = "";
      for (Modifier modifier : method.getModifiers()) {
         modifiers += modifier;
      }
      String signature = modifiers + name + "(" + ")";
      return signature;
   }
}
