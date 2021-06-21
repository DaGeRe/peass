package net.kieker.sourceinstrumentation.instrument;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationCodeBlocks;

public class SamplingBlockBuilder extends BlockBuilder {

   private final int count;
   
   public SamplingBlockBuilder(final AllowedKiekerRecord recordType, final int count) {
      super(recordType, false, false);
      this.count = count;
   }

   @Override
   public BlockStmt buildStatement(final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException("Not implemented yet (does Sampling + OperationExecutionRecord make sense?)");
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         return buildSelectiveSamplingStatement(originalBlock, addReturn, parameters);
      } else {
         throw new RuntimeException();
      }
   }
   
   @Override
   public BlockStmt buildEmptyConstructor(final SamplingParameters parameters) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException("Not implemented yet (does Sampling + OperationExecutionRecord make sense?)");
      } else if (recordType.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION)) {
         return buildConstructorStatement(parameters);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildSelectiveSamplingStatement(final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.SAMPLING.getBefore());

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(parameters.getFinalBlock(parameters.getSignature(), count));
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);

      return replacedStatement;
   }
   
   public BlockStmt buildConstructorStatement(final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.SAMPLING.getBefore());
      replacedStatement.addAndGetStatement(parameters.getFinalBlock(parameters.getSignature(), count));
      return replacedStatement;
   }
}
