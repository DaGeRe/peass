package de.peass.kiekerInstrument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.stmt.BlockStatement;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.execution.AllowedKiekerRecord;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.testtransformation.ParseUtil;
import javassist.compiler.ast.MethodDecl;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.registry.ControlFlowRegistry;
import kieker.monitoring.core.registry.SessionRegistry;

/**
 * Adds kieker monitoring code to existing source code *in-place*, i.e. the existing .java-files will get changed.
 * 
 * @author reichelt
 *
 */
public class InstrumentKiekerSource {

   private static final Logger LOG = LogManager.getLogger(InstrumentKiekerSource.class);

   private final AllowedKiekerRecord usedRecord;

   public InstrumentKiekerSource(AllowedKiekerRecord usedRecord) {
      this.usedRecord = usedRecord;
   }

   public void instrumentProject(File folder) throws IOException {
      for (File javaFile : FileUtils.listFiles(folder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
         LOG.info("Handling: " + javaFile);
         instrument(javaFile);
      }
   }

   public void instrument(File file) throws IOException {
      CompilationUnit unit = JavaParserProvider.parse(file); // TODO Package
      addImports(unit);

      ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
      String packageName = unit.getPackageDeclaration().get().getNameAsString();
      String name = packageName + "." + clazz.getNameAsString();

      handleChildren(clazz, name);

      Files.write(file.toPath(), unit.toString().getBytes(StandardCharsets.UTF_8));
   }

   private void addImports(CompilationUnit unit) {
      unit.addImport("kieker.monitoring.core.controller.MonitoringController");
      unit.addImport("kieker.monitoring.core.registry.ControlFlowRegistry");
      unit.addImport("kieker.monitoring.core.registry.SessionRegistry");
      unit.addImport(usedRecord.getRecord());
   }

   private void handleChildren(ClassOrInterfaceDeclaration clazz, String name) {
      for (Node child : clazz.getChildNodes()) {
         if (child instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) child;
            final Optional<BlockStmt> body = method.getBody();

            if (body.isPresent()) {
               BlockStmt originalBlock = body.get();
               String signature = getSignature(name + "." + method.getNameAsString(), method);

               BlockStmt replacedStatement = BlockBuilder.buildStatement(originalBlock, signature, method.getType().toString().equals("void"), usedRecord);

               method.setBody(replacedStatement);

            } else {
               LOG.info("Unable to instrument " + name + "." + method.getNameAsString() + " because it has no body");
            }
         } else if (child instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructor = (ConstructorDeclaration) child;
            final BlockStmt originalBlock = constructor.getBody();
            String signature = getSignature(name, constructor);

            BlockStmt replacedStatement = BlockBuilder.buildConstructorStatement(originalBlock, signature, true, usedRecord);

            constructor.setBody(replacedStatement);
         }
      }
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
