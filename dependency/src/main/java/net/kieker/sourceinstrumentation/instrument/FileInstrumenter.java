package net.kieker.sourceinstrumentation.instrument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.testtransformation.ParseUtil;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;

public class FileInstrumenter {

   private static final Logger LOG = LogManager.getLogger(FileInstrumenter.class);

   private final CompilationUnit unit;
   private final File file;

   private final InstrumentationConfiguration configuration;

   public FileInstrumenter(final File file, final InstrumentationConfiguration configuration) throws FileNotFoundException {
      this.unit = JavaParserProvider.parse(file);
      this.file = file;
      this.configuration = configuration;
   }

   public void instrument() throws IOException {
      Optional<PackageDeclaration> packageDeclaration = unit.getPackageDeclaration();
      final String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() + "." : "";
      boolean hasChanges = false;
      
      for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
         hasChanges |= handleTypeDeclaration(clazz, packageName);
      }
      
      for (EnumDeclaration enumDecl : ParseUtil.getEnums(unit)) {
         hasChanges |= handleTypeDeclaration(enumDecl, packageName);
      }
      
      if (hasChanges) {
         addImports(unit);
         Files.write(file.toPath(), unit.toString().getBytes(StandardCharsets.UTF_8));
      }
   }


   private boolean handleTypeDeclaration(final TypeDeclaration<?> type, final String packageName) throws IOException {
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, unit, type);
      boolean hasChanged = instrumenter.handleTypeDeclaration(type, packageName);
      return hasChanged;
   }

   private void addImports(final CompilationUnit unit) {
      unit.addImport("kieker.monitoring.core.controller.MonitoringController");
      unit.addImport("kieker.monitoring.core.registry.ControlFlowRegistry");
      unit.addImport("kieker.monitoring.core.registry.SessionRegistry");
      unit.addImport(configuration.getUsedRecord().getRecord());
   }

   
   
}
