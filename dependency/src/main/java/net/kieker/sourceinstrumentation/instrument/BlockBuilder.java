package net.kieker.sourceinstrumentation.instrument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
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
   private final boolean enableDeactivation, enableAdaptiveMonitoring;
   private boolean useStaticVariables = true;

   public BlockBuilder(final AllowedKiekerRecord recordType, final boolean enableDeactivation, final boolean enableAdaptiveMonitoring) {
      this.recordType = recordType;
      this.enableDeactivation = enableDeactivation;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
   }

   public BlockStmt buildConstructorStatement(final BlockStmt originalBlock, final boolean mayNeedReturn, final SamplingParameters parameters, final TypeDeclaration<?> type) {
      if (type.isEnumDeclaration()) {
         useStaticVariables = false;
      }
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
      useStaticVariables = true;
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

      new HeaderBuilder(useStaticVariables, enableDeactivation, enableAdaptiveMonitoring).buildHeader(originalBlock, signature, mayNeedReturn, replacedStatement);
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getBefore());

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getAfter());
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   public BlockStmt buildOperationExecutionStatement(final BlockStmt originalBlock, final String signature, final boolean mayNeedReturn) {
      BlockStmt replacedStatement = new BlockStmt();

      new HeaderBuilder(useStaticVariables, enableDeactivation, enableAdaptiveMonitoring).buildHeader(originalBlock, signature, mayNeedReturn, replacedStatement);

      String before = getCorrectStatement(InstrumentationCodeBlocks.OPERATIONEXECUTION.getBefore());
      replacedStatement.addAndGetStatement(before);
      BlockStmt finallyBlock = new BlockStmt();
      String after = getCorrectStatement(InstrumentationCodeBlocks.OPERATIONEXECUTION.getAfter());
      finallyBlock.addAndGetStatement(after);
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   private String getCorrectStatement(final String originalStatement) {
      String before;
      if (useStaticVariables) {
         before = originalStatement;
      } else {
         before = replaceStaticVariables(originalStatement);
      }
      return before;
   }

   public BlockStmt buildEmptyConstructor(final TypeDeclaration<?> type, final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         if (type.isEnumDeclaration()) {
            useStaticVariables = false;
         }
         buildOperationExecutionRecordDefaultConstructor(parameters.getSignature(), replacedStatement);
         useStaticVariables = true;
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         if (type.isEnumDeclaration()) {
            useStaticVariables = false;
         }
         buildReducedOperationExecutionRecordDefaultConstructor(parameters.getSignature(), replacedStatement);
         useStaticVariables = false;
      } else {
         throw new RuntimeException();
      }
      return replacedStatement;
   }

   private String replaceStaticVariables(final String original) {
      String before = original
            .replaceAll(InstrumentationConstants.PREFIX + "VM_NAME", "kieker.monitoring.core.controller.MonitoringController.getInstance().getHostname()")
            .replaceAll(InstrumentationConstants.PREFIX + "SESSION_REGISTRY", "SessionRegistry.INSTANCE")
            .replaceAll(InstrumentationConstants.PREFIX + "controlFlowRegistry", "ControlFlowRegistry.INSTANCE")
            .replaceAll(InstrumentationConstants.PREFIX + "controller", InstrumentationConstants.CONTROLLER_NAME)
            .replaceAll(InstrumentationConstants.PREFIX + "TIME_SOURCE", "kieker.monitoring.core.controller.MonitoringController.getInstance().getTimeSource()");
      return before;
   }

   private void buildEmptyConstructor(final String signature, final BlockStmt replacedStatement, final String before, final String after) {
      new HeaderBuilder(useStaticVariables, enableDeactivation, enableAdaptiveMonitoring).buildHeader(new BlockStmt(), signature, false, replacedStatement);
      if (useStaticVariables) {
         replacedStatement.addAndGetStatement(before);
         replacedStatement.addAndGetStatement(after);
      } else {
         replacedStatement.addAndGetStatement(replaceStaticVariables(before));
         replacedStatement.addAndGetStatement(replaceStaticVariables(after));
      }

   }

   private void buildReducedOperationExecutionRecordDefaultConstructor(final String signature, final BlockStmt replacedStatement) {
      buildEmptyConstructor(signature, replacedStatement,
            InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getBefore(), InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getAfter());
   }

   private void buildOperationExecutionRecordDefaultConstructor(final String signature, final BlockStmt replacedStatement) {
      buildEmptyConstructor(signature, replacedStatement,
            InstrumentationCodeBlocks.OPERATIONEXECUTION.getBefore(), InstrumentationCodeBlocks.OPERATIONEXECUTION.getAfter());
   }
}
