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
import org.apache.groovy.parser.antlr4.GroovyParser.ClassDeclarationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.stmt.BlockStatement;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

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
         LOG.debug("Instrumenting: " + javaFile);
         instrument(javaFile);
      }
   }


   public void instrument(File file) throws IOException {
      FileInstrumenter instrumenter = new FileInstrumenter(file, usedRecord, includedPatterns, blockBuilder, sample);
      instrumenter.instrument();
   }

  
}
