package net.kieker.sourceinstrumentation.instrument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationCodeBlocks;
import net.kieker.sourceinstrumentation.InstrumentationConstants;

public class BlockBuilder {

   private static final Logger LOG = LogManager.getLogger(BlockBuilder.class);

   protected final AllowedKiekerRecord recordType;
   private final boolean enableDeactivation;

   public BlockBuilder(final AllowedKiekerRecord recordType, final boolean enableDeactivation) {
      this.recordType = recordType;
      this.enableDeactivation = enableDeactivation;
   }

   public BlockStmt buildConstructorStatement(final BlockStmt originalBlock, final SamplingParameters parameters) {
      LOG.debug("Statements: " + originalBlock.getStatements().size() + " " + parameters.getSignature());
      final BlockStmt replacedStatement = new BlockStmt();
      final ExplicitConstructorInvocationStmt constructorStatement = findConstructorInvocation(originalBlock);
      if (constructorStatement != null) {
         replacedStatement.addAndGetStatement(constructorStatement);
         originalBlock.getStatements().remove(constructorStatement);
      }

      final BlockStmt regularChangedStatement = buildStatement(originalBlock, true, parameters);
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
      if (enableDeactivation) {
         replacedStatement.addAndGetStatement("if (!MonitoringController.getInstance().isMonitoringEnabled()) {\n" +
               originalBlock.toString() + "\n" +
               (addReturn ? "return;" : "") +
               "      }");
      }
      replacedStatement.addAndGetStatement("final String " + InstrumentationConstants.PREFIX + "signature = \"" + signature + "\";");
      if (enableDeactivation) {
         replacedStatement.addAndGetStatement("if (!MonitoringController.getInstance().isProbeActivated(" + InstrumentationConstants.PREFIX + "signature)) {\n" +
               originalBlock.toString() +
               (addReturn ? "return;" : "") +
               "      }");
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
