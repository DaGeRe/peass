package net.kieker.sourceinstrumentation;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import net.kieker.sourceinstrumentation.instrument.BlockBuilder;

public class TestBlockBuilder {
   
   @Test
   public void testRegularBuilding() {
      BlockStmt block = new BlockStmt();
      ExpressionStmt expr = new ExpressionStmt(new MethodCallExpr("System.out.println", new CharLiteralExpr("Hello World")));
      block.addStatement(expr);
      
      BlockBuilder builder = new BlockBuilder(AllowedKiekerRecord.OPERATIONEXECUTION, true, true);
      BlockStmt instrumented = builder.buildOperationExecutionStatement(block, "void MyClass.callHelloWorld", false);
      
      MatcherAssert.assertThat(instrumented.toString(), Matchers.containsString("long _kieker_sourceInstrumentation_tout = _kieker_sourceInstrumentation_TIME_SOURCE.getTime();"));
   }
}
