package de.peass.kiekerInstrument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import de.peass.dependency.execution.AllowedKiekerRecord;

public class BlockBuilder {

   private static final String BEFORE_OER_SOURCE = "      // collect data\n" +
         "      final boolean entrypoint;\n" +
         "      final String hostname = MonitoringController.getInstance().getHostname();\n" +
         "      final String sessionId = SessionRegistry.INSTANCE.recallThreadLocalSessionId();\n" +
         "      final int eoi; // this is executionOrderIndex-th execution in this trace\n" +
         "      final int ess; // this is the height in the dynamic call tree of this execution\n" +
         "      long traceId = ControlFlowRegistry.INSTANCE.recallThreadLocalTraceId(); // traceId, -1 if entry point\n" +
         "      if (traceId == -1) {\n" +
         "         entrypoint = true;\n" +
         "         traceId = ControlFlowRegistry.INSTANCE.getAndStoreUniqueThreadLocalTraceId();\n" +
         "         ControlFlowRegistry.INSTANCE.storeThreadLocalEOI(0);\n" +
         "         ControlFlowRegistry.INSTANCE.storeThreadLocalESS(1); // next operation is ess + 1\n" +
         "         eoi = 0;\n" +
         "         ess = 0;\n" +
         "      } else {\n" +
         "         entrypoint = false;\n" +
         "         eoi = ControlFlowRegistry.INSTANCE.incrementAndRecallThreadLocalEOI(); // ess > 1\n" +
         "         ess = ControlFlowRegistry.INSTANCE.recallAndIncrementThreadLocalESS(); // ess >= 0\n" +
         "         if ((eoi == -1) || (ess == -1)) {\n" +
         "            System.err.println(\"eoi and/or ess have invalid values: eoi == {} ess == {}\"+ eoi+ \"\" + ess);\n" +
         "            MonitoringController.getInstance().terminateMonitoring();\n" +
         "         }\n" +
         "      }\n" +
         "      // measure before\n" +
         "      final long tin = MonitoringController.getInstance().getTimeSource().getTime();\n";
   private static final String AFTER_OER_SOURCE = "// measure after\n" +
         "         final long tout = MonitoringController.getInstance().getTimeSource().getTime();\n" +
         "         MonitoringController.getInstance().newMonitoringRecord(new OperationExecutionRecord(signature, sessionId, traceId, tin, tout, hostname, eoi, ess));\n" +
         "         // cleanup\n" +
         "         if (entrypoint) {\n" +
         "            ControlFlowRegistry.INSTANCE.unsetThreadLocalTraceId();\n" +
         "            ControlFlowRegistry.INSTANCE.unsetThreadLocalEOI();\n" +
         "            ControlFlowRegistry.INSTANCE.unsetThreadLocalESS();\n" +
         "         } else {\n" +
         "            ControlFlowRegistry.INSTANCE.storeThreadLocalESS(ess); // next operation is ess\n" +
         "         }";

   private static final Logger LOG = LogManager.getLogger(BlockBuilder.class);

   protected final AllowedKiekerRecord recordType;
   private final boolean enableDeactivation;

   public BlockBuilder(final AllowedKiekerRecord recordType, final boolean enableDeactivation) {
      this.recordType = recordType;
      this.enableDeactivation = enableDeactivation;
   }

   public BlockStmt buildConstructorStatement(final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters) {
      LOG.debug("Statements: " + originalBlock.getStatements().size() + " " + parameters.getSignature());
      BlockStmt replacedStatement = new BlockStmt();
      ExplicitConstructorInvocationStmt constructorStatement = null;
      for (Statement st : originalBlock.getStatements()) {
         if (st instanceof ExplicitConstructorInvocationStmt) {
            constructorStatement = (ExplicitConstructorInvocationStmt) st;
         }
      }
      if (constructorStatement != null) {
         replacedStatement.addAndGetStatement(constructorStatement);
         originalBlock.getStatements().remove(constructorStatement);
      }

      final BlockStmt regularChangedStatement = buildStatement(originalBlock, addReturn, parameters);
      for (Statement st : regularChangedStatement.getStatements()) {
         replacedStatement.addAndGetStatement(st);
      }

      return replacedStatement;
   }

   public BlockStmt buildStatement(final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         return buildOperationExecutionStatement(originalBlock, parameters.getSignature(), addReturn);
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         return buildReducedOperationExecutionStatement(originalBlock, parameters.getSignature(), addReturn);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildReducedOperationExecutionStatement(final BlockStmt originalBlock, final String signature, final boolean addReturn) {
      BlockStmt replacedStatement = new BlockStmt();

      buildHeader(originalBlock, signature, addReturn, replacedStatement);
      replacedStatement.addAndGetStatement("      final long tin = MonitoringController.getInstance().getTimeSource().getTime();");

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement("// measure after\n");
      finallyBlock.addAndGetStatement("final long tout = MonitoringController.getInstance().getTimeSource().getTime()");
      finallyBlock.addAndGetStatement("MonitoringController.getInstance().newMonitoringRecord(new ReducedOperationExecutionRecord(signature, tin, tout))");
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   public BlockStmt buildOperationExecutionStatement(final BlockStmt originalBlock, final String signature, final boolean addReturn) {
      BlockStmt replacedStatement = new BlockStmt();

      buildHeader(originalBlock, signature, addReturn, replacedStatement);
      replacedStatement.addAndGetStatement(BEFORE_OER_SOURCE);
      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(AFTER_OER_SOURCE);
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   private void buildHeader(final BlockStmt originalBlock, final String signature, final boolean addReturn, final BlockStmt replacedStatement) {
      if (enableDeactivation) {
         replacedStatement.addAndGetStatement("if (!MonitoringController.getInstance().isMonitoringEnabled()) {\n" +
               originalBlock.toString() +
               (addReturn ? "return;" : "") +
               "      }");
      }
      replacedStatement.addAndGetStatement("final String signature = \"" + signature + "\";");
      if (enableDeactivation) {
         replacedStatement.addAndGetStatement("if (!MonitoringController.getInstance().isProbeActivated(signature)) {\n" +
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
   
   public BlockStmt buildEmptySamplingConstructor(final String signature, final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException("Not implemented yet (does Sampling + OperationExecutionRecord make sense?)");
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         buildHeader(replacedStatement, signature, false, replacedStatement);
         replacedStatement.addAndGetStatement("      final long tin = MonitoringController.getInstance().getTimeSource().getTime();");
         replacedStatement.addAndGetStatement(parameters.getFinalBlock(signature, 1000));
      } else {
         throw new RuntimeException();
      }
      return replacedStatement;
   }

   private void buildReducedOperationExecutionRecordDefaultConstructor(final String signature, final BlockStmt replacedStatement) {
      buildHeader(replacedStatement, signature, false, replacedStatement);
      replacedStatement.addAndGetStatement("      final long tin = MonitoringController.getInstance().getTimeSource().getTime();");
      replacedStatement.addAndGetStatement("// measure after\n");
      replacedStatement.addAndGetStatement("final long tout = MonitoringController.getInstance().getTimeSource().getTime()");
      replacedStatement.addAndGetStatement("MonitoringController.getInstance().newMonitoringRecord(new ReducedOperationExecutionRecord(signature, tin, tout))");
   }

   private void buildOperationExecutionRecordDefaultConstructor(final String signature, final BlockStmt replacedStatement) {
      buildHeader(new BlockStmt(), signature, false, replacedStatement);
      replacedStatement.addAndGetStatement(BEFORE_OER_SOURCE + AFTER_OER_SOURCE);
   }
}
