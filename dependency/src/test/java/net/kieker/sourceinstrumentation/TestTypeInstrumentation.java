package net.kieker.sourceinstrumentation;

import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import net.kieker.sourceinstrumentation.instrument.TypeInstrumenter;

public class TestTypeInstrumentation {
   
   @Test
   public void testBasicInstrumentation() throws IOException {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration();
      clazz.setName("MyClazz");
      MethodDeclaration method = clazz.addMethod("myMethod");
      method.setBody(simpleBlock);
      
      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, false);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class));
      boolean hasChange = instrumenter.handleTypeDeclaration(clazz, "de.dagere.test");
      
      Assert.assertTrue(hasChange);
   }
}
