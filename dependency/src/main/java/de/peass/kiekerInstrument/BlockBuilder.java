package de.peass.kiekerInstrument;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import de.peass.dependency.execution.AllowedKiekerRecord;

public class BlockBuilder {

   private final AllowedKiekerRecord recordType;
   private final boolean enableDeactivation;

   public BlockBuilder(final AllowedKiekerRecord recordType, final boolean enableDeactivation) {
      this.recordType = recordType;
      this.enableDeactivation = enableDeactivation;
   }

   public BlockStmt buildConstructorStatement(final BlockStmt originalBlock, final String signature, final boolean addReturn) {
      System.out.println("Statements: " + originalBlock.getStatements().size() + " " + signature);
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

      final BlockStmt regularChangedStatement = buildStatement(originalBlock, signature, addReturn);
      for (Statement st : regularChangedStatement.getStatements()) {
         replacedStatement.addAndGetStatement(st);
      }

      return replacedStatement;
   }

   public BlockStmt buildSampleStatement(BlockStmt originalBlock, String signature, boolean addReturn, String counterName, String sumName) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException("Not implemented yet");
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         return buildSelectiveSamplingStatement(originalBlock, signature, addReturn, counterName, sumName);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildStatement(BlockStmt originalBlock, String signature, boolean addReturn) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         return buildOperationExecutionStatement(originalBlock, signature, addReturn);
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         return buildReducedOperationExecutionStatement(originalBlock, signature, addReturn);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildSelectiveSamplingStatement(BlockStmt originalBlock, String signature, boolean addReturn, String samplingCounterName, String sumName) {
      BlockStmt replacedStatement = new BlockStmt();
      replacedStatement.addAndGetStatement("      final long tin = MonitoringController.getInstance().getTimeSource().getTime();");

      int count = 1000;

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement("// measure after\n" +
            "         final long tout = MonitoringController.getInstance().getTimeSource().getTime();\n" +
            "        " + sumName + "+=tout-tin;\n" +
            "if (" + samplingCounterName + "++%" + count + "==0){\n" +
            "final String signature = \"" + signature + "\";\n" +
            "final long calculatedTout=tin+"+sumName+";\n" +
            "MonitoringController.getInstance().newMonitoringRecord(new ReducedOperationExecutionRecord(signature, tin, calculatedTout));\n"
            + sumName + "=0;}\n");
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);

      return replacedStatement;
   }

   public BlockStmt buildReducedOperationExecutionStatement(BlockStmt originalBlock, String signature, boolean addReturn) {
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

   public BlockStmt buildOperationExecutionStatement(BlockStmt originalBlock, String signature, boolean addReturn) {
      BlockStmt replacedStatement = new BlockStmt();

      buildHeader(originalBlock, signature, addReturn, replacedStatement);
      replacedStatement.addAndGetStatement("      // collect data\n" +
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
            "      final long tin = MonitoringController.getInstance().getTimeSource().getTime();");
      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement("// measure after\n" +
            "         final long tout = MonitoringController.getInstance().getTimeSource().getTime();\n" +
            "         MonitoringController.getInstance().newMonitoringRecord(new OperationExecutionRecord(signature, sessionId, traceId, tin, tout, hostname, eoi, ess));\n" +
            "         // cleanup\n" +
            "         if (entrypoint) {\n" +
            "            ControlFlowRegistry.INSTANCE.unsetThreadLocalTraceId();\n" +
            "            ControlFlowRegistry.INSTANCE.unsetThreadLocalEOI();\n" +
            "            ControlFlowRegistry.INSTANCE.unsetThreadLocalESS();\n" +
            "         } else {\n" +
            "            ControlFlowRegistry.INSTANCE.storeThreadLocalESS(ess); // next operation is ess\n" +
            "         }");
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   private void buildHeader(BlockStmt originalBlock, String signature, boolean addReturn, BlockStmt replacedStatement) {
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
}
