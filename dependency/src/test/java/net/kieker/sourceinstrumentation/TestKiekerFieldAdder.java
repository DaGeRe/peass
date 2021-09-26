package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import net.kieker.sourceinstrumentation.instrument.KiekerFieldAdder;

public class TestKiekerFieldAdder {

   @Test
   public void testBasicAdding() throws IOException {
      File testFile = new File("src/test/resources/sourceInstrumentation/example_staticInitialization/src/main/java/de/peass/C0_0.java");

      KiekerFieldAdder adder = new KiekerFieldAdder(TestMonitoringConfiguration.CONFIGURATION_EXAMPLE);

      CompilationUnit unit = JavaParserProvider.parse(testFile);

      List<TypeDeclaration> types = unit.findAll(TypeDeclaration.class);
      TypeDeclaration<?> clazz = types.get(0);

      adder.addKiekerFields(clazz);

      System.out.println(clazz);

      FieldDeclaration field = clazz.getFields().get(0);
      MatcherAssert.assertThat(field.toString(), Matchers.containsString("kieker.monitoring.core.controller.IMonitoringController _kieker_sourceInstrumentation_controller"));
   }
}
