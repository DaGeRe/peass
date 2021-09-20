package net.kieker.sourceinstrumentation.instrument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.testtransformation.ParseUtil;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.InstrumentationConstants;

public class FileInstrumenter {

   private static final Logger LOG = LogManager.getLogger(FileInstrumenter.class);

   private final CompilationUnit unit;
   private final File file;

   private final InstrumentationConfiguration configuration;
   private final BlockBuilder blockBuilder;

   private boolean oneHasChanged = false;

   private int counterIndex = 0;
   private final List<String> countersToAdd = new LinkedList<>();
   private final List<String> sumsToAdd = new LinkedList<>();
   private SignatureMatchChecker checker;

   public FileInstrumenter(final File file, final InstrumentationConfiguration configuration, final BlockBuilder blockBuilder) throws FileNotFoundException {
      this.unit = JavaParserProvider.parse(file);
      this.file = file;
      this.configuration = configuration;
      this.blockBuilder = blockBuilder;
      checker = new SignatureMatchChecker(configuration.getIncludedPatterns(), configuration.getExcludedPatterns());
   }

   public void instrument() throws IOException {
      TypeDeclaration<?> clazz = ParseUtil.getClass(unit);
      Optional<PackageDeclaration> packageDeclaration = unit.getPackageDeclaration();
      final String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() + "." : "";
      handleTypeDeclaration(clazz, packageName);
      TypeDeclaration<?> enumDecl = ParseUtil.getEnum(unit);
      handleTypeDeclaration(enumDecl, packageName);
   }

   private void handleTypeDeclaration(final TypeDeclaration<?> clazz, final String packageName) throws IOException {
      if (clazz != null) {
         final String name = packageName + clazz.getNameAsString();

         boolean fileContainsChange = handleChildren(clazz, name);

         if (fileContainsChange) {
            for (String counterName : countersToAdd) {
               clazz.addField("int", counterName, Keyword.PRIVATE, Keyword.STATIC);
            }
            for (String counterName : sumsToAdd) {
               clazz.addField("long", counterName, Keyword.PRIVATE, Keyword.STATIC);
            }
            addImports(unit);
            clazz.addFieldWithInitializer("kieker.monitoring.core.controller.IMonitoringController", InstrumentationConstants.PREFIX + "controller",
                  new MethodCallExpr("kieker.monitoring.core.controller.MonitoringController.getInstance"),
                  Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL);

            clazz.addFieldWithInitializer("kieker.monitoring.timer.ITimeSource", InstrumentationConstants.PREFIX + "TIME_SOURCE",
                  new MethodCallExpr(InstrumentationConstants.PREFIX + "controller.getTimeSource"),
                  Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL);

            if (configuration.getUsedRecord() == AllowedKiekerRecord.OPERATIONEXECUTION) {
               clazz.addFieldWithInitializer("String", InstrumentationConstants.PREFIX + "VM_NAME",
                     new MethodCallExpr(InstrumentationConstants.PREFIX + "controller.getHostname"),
                     Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL);

               clazz.addFieldWithInitializer("kieker.monitoring.core.registry.SessionRegistry", InstrumentationConstants.PREFIX + "SESSION_REGISTRY",
                     new FieldAccessExpr(new NameExpr("SessionRegistry"), "INSTANCE"),
                     Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL);

               clazz.addFieldWithInitializer("kieker.monitoring.core.registry.ControlFlowRegistry", InstrumentationConstants.PREFIX + "controlFlowRegistry",
                     new FieldAccessExpr(new NameExpr("ControlFlowRegistry"), "INSTANCE"),
                     Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL);
            }

            Files.write(file.toPath(), unit.toString().getBytes(StandardCharsets.UTF_8));

         }
      }
   }

   private void addImports(final CompilationUnit unit) {
      unit.addImport("kieker.monitoring.core.controller.MonitoringController");
      unit.addImport("kieker.monitoring.core.registry.ControlFlowRegistry");
      unit.addImport("kieker.monitoring.core.registry.SessionRegistry");
      unit.addImport(configuration.getUsedRecord().getRecord());
   }

   private boolean handleChildren(final TypeDeclaration<?> clazz, final String name) {
      boolean constructorFound = false;
      for (Node child : clazz.getChildNodes()) {
         if (child instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) child;
            if (clazz instanceof ClassOrInterfaceDeclaration) {
               ClassOrInterfaceDeclaration declaringEntity = (ClassOrInterfaceDeclaration) clazz;
               if (!declaringEntity.isInterface() || method.getBody().isPresent()) {
                  instrumentMethod(name, method);
               }
            } else {
               instrumentMethod(name, method);
            }
         } else if (child instanceof ConstructorDeclaration) {
            instrumentConstructor(clazz, name, child);
            constructorFound = true;
         } else if (child instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration innerClazz = (ClassOrInterfaceDeclaration) child;
            String innerName = innerClazz.getNameAsString();
            handleChildren(innerClazz, name + "$" + innerName);
         }
      }
      if (!constructorFound && configuration.isCreateDefaultConstructor()) {
         if (clazz instanceof EnumDeclaration) {
            createDefaultConstructor(clazz, name, Modifier.Keyword.PRIVATE);
         } else if (clazz instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration clazzDecl = (ClassOrInterfaceDeclaration) clazz;
            if (!clazzDecl.isInterface()) {
               createDefaultConstructor(clazz, name, Modifier.Keyword.PUBLIC);
            }
         }
      }
      return oneHasChanged;
   }

   private void createDefaultConstructor(final TypeDeclaration<?> clazz, final String name, final Keyword visibility) {
      SignatureReader reader = new SignatureReader(unit, name);
      String signature = reader.getDefaultConstructor(clazz);
      if (checker.testSignatureMatch(signature)) {
         oneHasChanged = true;
         final SamplingParameters parameters = createParameters(signature);
         BlockStmt constructorBlock = blockBuilder.buildEmptyConstructor(parameters);
         ConstructorDeclaration constructor = clazz.addConstructor(visibility);
         constructor.setBody(constructorBlock);
      }
   }

   private void instrumentConstructor(final TypeDeclaration<?> clazz, final String name, final Node child) {
      final ConstructorDeclaration constructor = (ConstructorDeclaration) child;
      final BlockStmt originalBlock = constructor.getBody();
      final SignatureReader reader = new SignatureReader(unit, name);
      final String signature = reader.getSignature(clazz, constructor);
      final boolean oneMatches = checker.testSignatureMatch(signature);
      if (oneMatches) {
         final SamplingParameters parameters = createParameters(signature);

         boolean configurationRequiresReturn = configuration.isEnableAdaptiveMonitoring() || configuration.isEnableDeactivation();
         final BlockStmt replacedStatement = blockBuilder.buildConstructorStatement(originalBlock, configurationRequiresReturn, parameters);

         constructor.setBody(replacedStatement);
         oneHasChanged = true;
      }
   }

   private SamplingParameters createParameters(final String signature) {
      final SamplingParameters parameters = new SamplingParameters(signature, counterIndex);
      if (configuration.isSample()) {
         countersToAdd.add(parameters.getCounterName());
         sumsToAdd.add(parameters.getSumName());
         counterIndex++;
      }
      return parameters;
   }

   private int instrumentMethod(final String name, final MethodDeclaration method) {
      final Optional<BlockStmt> body = method.getBody();

      if (body.isPresent()) {
         BlockStmt originalBlock = body.get();
         SignatureReader reader = new SignatureReader(unit, name + "." + method.getNameAsString());
         String signature = reader.getSignature(method);
         boolean oneMatches = checker.testSignatureMatch(signature);
         if (oneMatches) {
            boolean configurationRequiresReturn = configuration.isEnableAdaptiveMonitoring() || configuration.isEnableDeactivation();
            final boolean needsReturn = method.getType().toString().equals("void") && configurationRequiresReturn;
            final SamplingParameters parameters = createParameters(signature);

            final BlockStmt replacedStatement = blockBuilder.buildStatement(originalBlock, needsReturn, parameters);

            method.setBody(replacedStatement);
            oneHasChanged = true;
         }
      } else {
         if (!method.isAbstract()) {
            LOG.info("Unable to instrument " + name + "." + method.getNameAsString() + " because it has no body");
         }
      }
      return counterIndex;
   }
}
