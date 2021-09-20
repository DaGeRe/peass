package net.kieker.sourceinstrumentation.instrument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationCodeBlocks;
import net.kieker.sourceinstrumentation.InstrumentationConstants;

public class BlockBuilder {

   private static final Logger LOG = LogManager.getLogger(BlockBuilder.class);

   protected final AllowedKiekerRecord recordType;
   private final boolean enableDeactivation, enableAdaptiveMonitoring;

   public BlockBuilder(final AllowedKiekerRecord recordType, final boolean enableDeactivation, final boolean enableAdaptiveMonitoring) {
      this.recordType = recordType;
      this.enableDeactivation = enableDeactivation;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
   }

   public BlockStmt buildConstructorStatement(final BlockStmt originalBlock, final boolean mayNeedReturn, final SamplingParameters parameters) {
      LOG.trace("Statements: " + originalBlock.getStatements().size() + " " + parameters.getSignature());
      final BlockStmt replacedStatement = new BlockStmt();
      final ExplicitConstructorInvocationStmt constructorStatement = findConstructorInvocation(originalBlock);
      if (constructorStatement != null) {
         replacedStatement.addAndGetStatement(constructorStatement);
         originalBlock.getStatements().remove(constructorStatement);
      }

      final BlockStmt regularChangedStatement = buildStatement(originalBlock, mayNeedReturn, parameters);
      for (Statement st : regularChangedStatement.getStatements()) {
         replacedStatement.addAndGetStatement(st);
      }

      return replacedStatement;
   }

   private ExplicitConstructorInvocationStmt findConstructorInvocation(final BlockStmt originalBlock) {
      ExplicitConstructorInvocationStmt constructorStatement = null;
      for (Statement st : originalBlock.getStatements()) {
         if (st instanceof ExplicitConstructorInvocationStmt) {
            constructorStatement = (ExplicitConstructorInvocationStmt) st;
         }
      }
      return constructorStatement;
   }

   public BlockStmt buildStatement(final BlockStmt originalBlock, final boolean mayNeedReturn, final SamplingParameters parameters) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         return buildOperationExecutionStatement(originalBlock, parameters.getSignature(), mayNeedReturn);
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         return buildReducedOperationExecutionStatement(originalBlock, parameters.getSignature(), mayNeedReturn);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildReducedOperationExecutionStatement(final BlockStmt originalBlock, final String signature, final boolean mayNeedReturn) {
      BlockStmt replacedStatement = new BlockStmt();

      buildHeader(originalBlock, signature, mayNeedReturn, replacedStatement);
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getBefore());

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getAfter());
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   public BlockStmt buildOperationExecutionStatement(final BlockStmt originalBlock, final String signature, final boolean mayNeedReturn) {
      BlockStmt replacedStatement = new BlockStmt();

      buildHeader(originalBlock, signature, mayNeedReturn, replacedStatement);
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.OPERATIONEXECUTION.getBefore());
      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(InstrumentationCodeBlocks.OPERATIONEXECUTION.getAfter());
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   private void buildHeader(final BlockStmt originalBlock, final String signature, final boolean needsReturn, final BlockStmt replacedStatement) {
      boolean afterUnreachable = ReachabilityDecider.isAfterUnreachable(originalBlock);

      boolean addReturn = needsReturn && !afterUnreachable;
      BlockStmt changed = addReturn ? originalBlock.addStatement("return;") : originalBlock;

      if (enableDeactivation) {
         Expression expr = new MethodCallExpr("!" + InstrumentationConstants.PREFIX + "controller.isMonitoringEnabled");
         IfStmt ifS = new IfStmt(expr, changed, null);
         replacedStatement.addStatement(ifS);
      }
      replacedStatement.addAndGetStatement("final String " + InstrumentationConstants.PREFIX + "signature = \"" + signature + "\";");
      if (enableAdaptiveMonitoring) {
         NameExpr name = new NameExpr(InstrumentationConstants.PREFIX + "signature");
         Expression expr = new MethodCallExpr("!" + InstrumentationConstants.PREFIX + "controller.isProbeActivated", name);
         IfStmt ifS = new IfStmt(expr, changed, null);
         replacedStatement.addStatement(ifS);
      }
   }

   public BlockStmt buildEmptyConstructor(final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         buildOperationExecutionRecordDefaultConstructor(parameters.getSignature(), replacedStatement);
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         buildReducedOperationExecutionRecordDefaultConstructor(parameters.getSignature(), replacedStatement);
      } else {
         throw new RuntimeException();
      }
      return replacedStatement;
   }

   private void buildReducedOperationExecutionRecordDefaultConstructor(final String signature, final BlockStmt replacedStatement) {
      buildHeader(new BlockStmt(), signature, false, replacedStatement);
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getBefore());
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getAfter());
   }

   private void buildOperationExecutionRecordDefaultConstructor(final String signature, final BlockStmt replacedStatement) {
      buildHeader(new BlockStmt(), signature, false, replacedStatement);
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.OPERATIONEXECUTION.getBefore());
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.OPERATIONEXECUTION.getAfter());
   }
}
