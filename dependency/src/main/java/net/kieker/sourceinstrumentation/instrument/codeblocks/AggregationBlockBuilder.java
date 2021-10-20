package net.kieker.sourceinstrumentation.instrument.codeblocks;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationCodeBlocks;
import net.kieker.sourceinstrumentation.instrument.SamplingParameters;

public class AggregationBlockBuilder extends BlockBuilder {

   private final int count;

   public AggregationBlockBuilder(final AllowedKiekerRecord recordType, final int count) {
      super(recordType, false, false);
      this.count = count;
   }

   @Override
   public BlockStmt buildStatement(final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters, final CodeBlockTransformer transformer) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException(
               "Not implemented yet (Aggregation + OperationExecutionRecord does not make sense, since OperationExecutionRecord contains too complex metadata for sampling)");
      } else if (recordType.equals(AllowedKiekerRecord.DURATION)) {
         if (!useStaticVariables) {
            return super.buildStatement(originalBlock, addReturn, parameters, transformer);
         } else {
            return buildSelectiveSamplingStatement(originalBlock, addReturn, parameters);
         }
      } else {
         throw new RuntimeException();
      }
   }

   @Override
   public BlockStmt buildEmptyConstructor(final TypeDeclaration<?> type, final SamplingParameters parameters, final CodeBlockTransformer transformer) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException(
               "Not implemented yet (Aggregation + OperationExecutionRecord does not make sense, since OperationExecutionRecord contains too complex metadata for sampling)");
      } else if (recordType.equals(AllowedKiekerRecord.DURATION)) {
         return buildConstructorStatement(parameters);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildSelectiveSamplingStatement(final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.AGGREGATION.getBefore());

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(parameters.getFinalBlock(parameters.getSignature(), count));
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);

      return replacedStatement;
   }

   public BlockStmt buildConstructorStatement(final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.AGGREGATION.getBefore());
      replacedStatement.addAndGetStatement(parameters.getFinalBlock(parameters.getSignature(), count));
      return replacedStatement;
   }
}
