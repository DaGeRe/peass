package net.kieker.sourceinstrumentation;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import net.kieker.sourceinstrumentation.instrument.codeblocks.BlockBuilder;
import net.kieker.sourceinstrumentation.instrument.codeblocks.CodeBlockTransformer;

public class TestBlockBuilder {
   
   @Test
   public void testRegularBuilding() {
      BlockStmt block = buildSimpleBlock();
      
      BlockBuilder builder = new BlockBuilder(AllowedKiekerRecord.OPERATIONEXECUTION, true, true);
      ClassOrInterfaceDeclaration mockedDeclaration = Mockito.mock(ClassOrInterfaceDeclaration.class);
      Mockito.when(mockedDeclaration.getNameAsString()).thenReturn("MyTest");
      CodeBlockTransformer transformer = new CodeBlockTransformer(mockedDeclaration);
      BlockStmt instrumented = builder.buildOperationExecutionStatement(block, "void MyClass.callHelloWorld", false, transformer);
      
      MatcherAssert.assertThat(instrumented.toString(), Matchers.containsString("long _kieker_sourceInstrumentation_tout = MyTest._kieker_sourceInstrumentation_TIME_SOURCE.getTime();"));
   }

   public static BlockStmt buildSimpleBlock() {
      BlockStmt block = new BlockStmt();
      ExpressionStmt expr = new ExpressionStmt(new MethodCallExpr("System.out.println", new CharLiteralExpr("Hello World")));
      block.addStatement(expr);
      return block;
   }
}
