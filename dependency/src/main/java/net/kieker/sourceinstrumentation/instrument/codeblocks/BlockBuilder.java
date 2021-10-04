package net.kieker.sourceinstrumentation.instrument.codeblocks;

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
import net.kieker.sourceinstrumentation.instrument.SamplingParameters;

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

   public BlockStmt buildConstructorStatement(final BlockStmt originalBlock, final boolean mayNeedReturn, final SamplingParameters parameters, final TypeDeclaration<?> type,
         final CodeBlockTransformer transformer) {
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

      final BlockStmt regularChangedStatement = buildStatement(originalBlock, mayNeedReturn, parameters, transformer);
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

   public BlockStmt buildStatement(final BlockStmt originalBlock, final boolean mayNeedReturn, final SamplingParameters parameters, final CodeBlockTransformer transformer) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         return buildOperationExecutionStatement(originalBlock, parameters.getSignature(), mayNeedReturn, transformer);
      } else if (recordType.equals(AllowedKiekerRecord.DURATION)) {
         return buildDurationStatement(originalBlock, parameters.getSignature(), mayNeedReturn, transformer);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildDurationStatement(final BlockStmt originalBlock, final String signature, final boolean mayNeedReturn, final CodeBlockTransformer transformer) {
      BlockStmt replacedStatement = new BlockStmt();

      new HeaderBuilder(useStaticVariables, enableDeactivation, enableAdaptiveMonitoring, transformer).buildHeader(originalBlock, signature, mayNeedReturn, replacedStatement);
      replacedStatement.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getBefore());

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getAfter());
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   public BlockStmt buildOperationExecutionStatement(final BlockStmt originalBlock, final String signature, final boolean mayNeedReturn, final CodeBlockTransformer transformer) {
      BlockStmt replacedStatement = new BlockStmt();

      new HeaderBuilder(useStaticVariables, enableDeactivation, enableAdaptiveMonitoring, transformer).buildHeader(originalBlock, signature, mayNeedReturn, replacedStatement);

      String before = transformer.getTransformedBlock(InstrumentationCodeBlocks.OPERATIONEXECUTION.getBefore(), useStaticVariables);
      replacedStatement.addAndGetStatement(before);
      BlockStmt finallyBlock = new BlockStmt();
      String after = transformer.getTransformedBlock(InstrumentationCodeBlocks.OPERATIONEXECUTION.getAfter(), useStaticVariables);
      finallyBlock.addAndGetStatement(after);
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);
      return replacedStatement;
   }

   public BlockStmt buildEmptyConstructor(final TypeDeclaration<?> type, final SamplingParameters parameters, final CodeBlockTransformer transformer) {
      BlockStmt replacedStatement = new BlockStmt();
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         if (type.isEnumDeclaration()) {
            useStaticVariables = false;
         }
         buildEmptyConstructor(parameters.getSignature(), 
               replacedStatement,
               InstrumentationCodeBlocks.OPERATIONEXECUTION.getBefore(), 
               InstrumentationCodeBlocks.OPERATIONEXECUTION.getAfter(),
               transformer);
         useStaticVariables = true;
      } else if (recordType.equals(AllowedKiekerRecord.DURATION)) {
         if (type.isEnumDeclaration()) {
            useStaticVariables = false;
         }
         buildEmptyConstructor(parameters.getSignature(), 
               replacedStatement,
               InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getBefore(), 
               InstrumentationCodeBlocks.REDUCED_OPERATIONEXECUTION.getAfter(),
               transformer);
         useStaticVariables = false;
      } else {
         throw new RuntimeException();
      }
      return replacedStatement;
   }

   private void buildEmptyConstructor(final String signature, final BlockStmt replacedStatement, final String before, final String after, final CodeBlockTransformer transformer) {
      new HeaderBuilder(useStaticVariables, enableDeactivation, enableAdaptiveMonitoring, transformer).buildHeader(new BlockStmt(), signature, false, replacedStatement);
      replacedStatement.addAndGetStatement(transformer.getTransformedBlock(before, useStaticVariables));
      replacedStatement.addAndGetStatement(transformer.getTransformedBlock(after, useStaticVariables));
   }

}
