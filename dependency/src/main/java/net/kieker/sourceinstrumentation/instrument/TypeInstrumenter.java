package net.kieker.sourceinstrumentation.instrument;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.InstrumentationConstants;
import net.kieker.sourceinstrumentation.instrument.codeblocks.BlockBuilder;
import net.kieker.sourceinstrumentation.instrument.codeblocks.CodeBlockTransformer;

public class TypeInstrumenter {

   private static final Logger LOG = LogManager.getLogger(TypeInstrumenter.class);

   private final InstrumentationConfiguration configuration;

   private final BlockBuilder blockBuilder;

   private int counterIndex = 0;
   private final List<String> countersToAdd = new LinkedList<>();
   private final List<String> sumsToAdd = new LinkedList<>();
   private final SignatureMatchChecker checker;
   private final CompilationUnit unit;
   private final TypeDeclaration<?> topLevelType;
   private final CodeBlockTransformer transformer;

   private boolean oneHasChanged = false;

   public TypeInstrumenter(final InstrumentationConfiguration configuration, final CompilationUnit unit, final TypeDeclaration<?> topLevelType) {
      this.configuration = configuration;
      this.blockBuilder = configuration.getBlockBuilder();
      this.checker = new SignatureMatchChecker(configuration.getIncludedPatterns(), configuration.getExcludedPatterns());
      this.unit = unit;
      this.topLevelType = topLevelType;
      transformer = new CodeBlockTransformer(topLevelType);
   }

   public boolean handleTypeDeclaration(final TypeDeclaration<?> type, final String packageName) throws IOException {
      if (type != null) {
         final String name = packageName + type.getNameAsString();

         boolean fileContainsChange = handleChildren(type, name);

         if (fileContainsChange) {
            for (String counterName : countersToAdd) {
               type.addField("int", counterName, Keyword.PRIVATE, Keyword.STATIC);
            }
            for (String counterName : sumsToAdd) {
               type.addField("long", counterName, Keyword.PRIVATE, Keyword.STATIC);
            }

            if (type == topLevelType) {
               new KiekerFieldAdder(configuration).addKiekerFields(type);
            }

            return true;
         }
      }
      return false;
   }

   private boolean handleChildren(final TypeDeclaration<?> clazz, final String name) {
      List<MethodDeclaration> methodsToAdd = new LinkedList<>();
      boolean constructorFound = false;
      for (ListIterator<Node> childIterator = clazz.getChildNodes().listIterator(); childIterator.hasNext();) {
         Node child = childIterator.next();
         if (child instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) child;
            if (clazz instanceof ClassOrInterfaceDeclaration) {
               ClassOrInterfaceDeclaration declaringEntity = (ClassOrInterfaceDeclaration) clazz;
               if (!declaringEntity.isInterface() || method.getBody().isPresent()) {
                  if (configuration.isExtractMethod()) {
                     extractMethod(methodsToAdd, method);
                  }
                  instrumentMethod(name, method);
               }
            } else {
               instrumentMethod(name, method);
            }
         } else if (child instanceof ConstructorDeclaration) {
            instrumentConstructor(clazz, name, child);
            constructorFound = true;
         } else if (child instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration innerClazz = (ClassOrInterfaceDeclaration) child;
            String innerName = innerClazz.getNameAsString();
            handleChildren(innerClazz, name + "$" + innerName);
         }
      }
      addExtractedWorkloadMethods(clazz, methodsToAdd);
      handleDefaultConstructor(clazz, name, constructorFound);
      return oneHasChanged;
   }

   private void extractMethod(final List<MethodDeclaration> methodsToAdd, final MethodDeclaration method) {
      String generatedName = generateWorkloadMethod(methodsToAdd, method);

      BlockStmt newBody = new BlockStmt();
      ExpressionStmt exprStatment;
      NodeList<Expression> parameterCallList = new NodeList<>();
      method.getParameters().stream().forEach(parameter -> parameterCallList.add(parameter.getNameAsExpression()));
      if (method.getType().toString().equals("void")) {
         exprStatment = new ExpressionStmt(new MethodCallExpr(generatedName, parameterCallList.toArray(new Expression[0])));
      } else {
         exprStatment = new ExpressionStmt(new MethodCallExpr("return " + generatedName, parameterCallList.toArray(new Expression[0])));
      }
      newBody.addStatement(exprStatment);
      method.setBody(newBody);
   }

   private String generateWorkloadMethod(final List<MethodDeclaration> methodsToAdd, final MethodDeclaration method) {
      String generatedName = InstrumentationConstants.PREFIX + method.getNameAsString();
      MethodDeclaration extracted = new MethodDeclaration();
      extracted.setName(generatedName);
      extracted.setType(method.getType());
      extracted.setBody(method.getBody().get());
      extracted.getParameters().addAll(method.getParameters());
      if (method.isStatic()) {
         extracted.setStatic(true);
      }

      methodsToAdd.add(extracted);
      return generatedName;
   }

   private void addExtractedWorkloadMethods(final TypeDeclaration<?> clazz, final List<MethodDeclaration> methodsToAdd) {
      for (MethodDeclaration methodToAdd : methodsToAdd) {
         MethodDeclaration addedMethod = clazz.addMethod(methodToAdd.getNameAsString(), Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
         if (methodToAdd.isStatic()) {
            addedMethod.setStatic(true);
         }
         addedMethod.getParameters().addAll(methodToAdd.getParameters());
         addedMethod.setBody(methodToAdd.getBody().get());
         addedMethod.setType(methodToAdd.getType());
      }
   }

   private void handleDefaultConstructor(final TypeDeclaration<?> clazz, final String name, final boolean constructorFound) {
      if (!constructorFound && configuration.isCreateDefaultConstructor()) {
         if (clazz instanceof EnumDeclaration) {
            createDefaultConstructor(clazz, name, Modifier.Keyword.PRIVATE);
         } else if (clazz instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration clazzDecl = (ClassOrInterfaceDeclaration) clazz;
            if (!clazzDecl.isInterface()) {
               createDefaultConstructor(clazz, name, Modifier.Keyword.PUBLIC);
            }
         }
      }
   }

   private void createDefaultConstructor(final TypeDeclaration<?> type, final String name, final Keyword visibility) {
      SignatureReader reader = new SignatureReader(unit, name);
      String signature = reader.getDefaultConstructor(type);
      if (checker.testSignatureMatch(signature)) {
         oneHasChanged = true;
         final SamplingParameters parameters = createParameters(signature);
         BlockStmt constructorBlock = blockBuilder.buildEmptyConstructor(type, parameters, transformer);
         ConstructorDeclaration constructor = type.addConstructor(visibility);
         constructor.setBody(constructorBlock);
      }
   }

   private void instrumentConstructor(final TypeDeclaration<?> type, final String name, final Node child) {
      final ConstructorDeclaration constructor = (ConstructorDeclaration) child;
      final BlockStmt originalBlock = constructor.getBody();
      final SignatureReader reader = new SignatureReader(unit, name);
      final String signature = reader.getSignature(type, constructor);
      final boolean oneMatches = checker.testSignatureMatch(signature);
      if (oneMatches) {
         final SamplingParameters parameters = createParameters(signature);

         boolean configurationRequiresReturn = configuration.isEnableAdaptiveMonitoring() || configuration.isEnableDeactivation();
         final BlockStmt replacedStatement = blockBuilder.buildConstructorStatement(originalBlock, configurationRequiresReturn, parameters, type, transformer);

         constructor.setBody(replacedStatement);
         oneHasChanged = true;
      }
   }

   private int instrumentMethod(final String name, final MethodDeclaration method) {
      final Optional<BlockStmt> body = method.getBody();

      if (body.isPresent()) {
         BlockStmt originalBlock = body.get();
         SignatureReader reader = new SignatureReader(unit, name + "." + method.getNameAsString());
         String signature = reader.getSignature(method);
         boolean oneMatches = checker.testSignatureMatch(signature);
         if (oneMatches) {
            boolean configurationRequiresReturn = configuration.isEnableAdaptiveMonitoring() || configuration.isEnableDeactivation();
            final boolean needsReturn = method.getType().toString().equals("void") && configurationRequiresReturn;
            final SamplingParameters parameters = createParameters(signature);

            final BlockStmt replacedStatement = blockBuilder.buildStatement(originalBlock, needsReturn, parameters, transformer);

            method.setBody(replacedStatement);
            oneHasChanged = true;
         }
      } else {
         if (!method.isAbstract()) {
            LOG.info("Unable to instrument " + name + "." + method.getNameAsString() + " because it has no body");
         }
      }
      return counterIndex;
   }

   private SamplingParameters createParameters(final String signature) {
      final SamplingParameters parameters = new SamplingParameters(signature, counterIndex);
      if (configuration.isSample()) {
         countersToAdd.add(parameters.getCounterName());
         sumsToAdd.add(parameters.getSumName());
         counterIndex++;
      }
      return parameters;
   }

}
