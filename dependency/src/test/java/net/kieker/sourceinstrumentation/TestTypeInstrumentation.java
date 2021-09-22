package net.kieker.sourceinstrumentation;

import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import net.kieker.sourceinstrumentation.instrument.TypeInstrumenter;

public class TestTypeInstrumentation {

   @Test
   public void testBasicInstrumentation() throws IOException {
      ClassOrInterfaceDeclaration clazz = buildClass();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, false);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class));
      boolean hasChange = instrumenter.handleTypeDeclaration(clazz, "de.dagere.test");

      Assert.assertTrue(hasChange);

      MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("long _kieker_sourceInstrumentation_tout = _kieker_sourceInstrumentation_TIME_SOURCE.getTime();"));
   }

   @Test
   public void testExtractionInstrumentation() throws IOException {
      ClassOrInterfaceDeclaration clazz = buildClass();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, true);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class));
      boolean hasChange = instrumenter.handleTypeDeclaration(clazz, "de.dagere.test");

      Assert.assertTrue(hasChange);

      MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("private final int " + InstrumentationConstants.PREFIX + "myMethod()"));
      System.out.println(clazz.toString());
   }

   private ClassOrInterfaceDeclaration buildClass() {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      simpleBlock.addStatement(new ReturnStmt("return 5"));
      ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration();
      clazz.setName("MyClazz");
      MethodDeclaration method = clazz.addMethod("myMethod");
      method.setBody(simpleBlock);
      method.setType("int");
      return clazz;
   }
}
