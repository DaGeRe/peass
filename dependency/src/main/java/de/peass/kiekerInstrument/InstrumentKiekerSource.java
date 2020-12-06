package de.peass.kiekerInstrument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.stmt.BlockStatement;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.execution.AllowedKiekerRecord;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.testtransformation.ParseUtil;
import javassist.compiler.ast.MethodDecl;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.probe.aspectj.operationExecution.OperationExecutionRecord;

/**
 * Adds kieker monitoring code to existing source code *in-place*, i.e. the existing .java-files will get changed.
 * 
 * @author reichelt
 *
 */
public class InstrumentKiekerSource {

   private static final Logger LOG = LogManager.getLogger(InstrumentKiekerSource.class);

   private final AllowedKiekerRecord usedRecord;

   public InstrumentKiekerSource(AllowedKiekerRecord usedRecord) {
      this.usedRecord = usedRecord;
   }

   public void instrument(File file) throws IOException {
      CompilationUnit unit = JavaParserProvider.parse(file); // TODO Package
      ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
      String packageName = unit.getPackageDeclaration().get().getNameAsString();
      String name = packageName + "." + clazz.getNameAsString();

      handleChildren(clazz, name);

      Files.write(file.toPath(), unit.toString().getBytes(StandardCharsets.UTF_8));
   }

   private void handleChildren(ClassOrInterfaceDeclaration clazz, String name) {
      for (Node child : clazz.getChildNodes()) {
         if (child instanceof MethodDeclaration) {
            
            MethodDeclaration method = (MethodDeclaration) child;
            name = name + "." + method.getNameAsString();
            final Optional<BlockStmt> body = method.getBody();

            if (body.isPresent()) {
               BlockStmt originalBlock = body.get();
               BlockStmt replacedStatement = new BlockStmt();
               replacedStatement.addAndGetStatement("if (!MonitoringController.getInstance().isMonitoringEnabled()) {\n" +
                     "         return thisJoinPoint.proceed();\n" +
                     "      }");
               replacedStatement.addAndGetStatement("final String signature = this.signatureToLongString(\"" + name + "\");");
               replacedStatement.addAndGetStatement("if (!CTRLINST.isProbeActivated(signature)) {\n" + 
                     "         return thisJoinPoint.proceed();\n" + 
                     "      }\n" + 
                     "      // collect data\n" + 
                     "      final boolean entrypoint;\n" + 
                     "      final String hostname = VMNAME;\n" + 
                     "      final String sessionId = SESSIONREGISTRY.recallThreadLocalSessionId();\n" + 
                     "      final int eoi; // this is executionOrderIndex-th execution in this trace\n" + 
                     "      final int ess; // this is the height in the dynamic call tree of this execution\n" + 
                     "      long traceId = CFREGISTRY.recallThreadLocalTraceId(); // traceId, -1 if entry point\n" + 
                     "      if (traceId == -1) {\n" + 
                     "         entrypoint = true;\n" + 
                     "         traceId = CFREGISTRY.getAndStoreUniqueThreadLocalTraceId();\n" + 
                     "         CFREGISTRY.storeThreadLocalEOI(0);\n" + 
                     "         CFREGISTRY.storeThreadLocalESS(1); // next operation is ess + 1\n" + 
                     "         eoi = 0;\n" + 
                     "         ess = 0;\n" + 
                     "      } else {\n" + 
                     "         entrypoint = false;\n" + 
                     "         eoi = CFREGISTRY.incrementAndRecallThreadLocalEOI(); // ess > 1\n" + 
                     "         ess = CFREGISTRY.recallAndIncrementThreadLocalESS(); // ess >= 0\n" + 
                     "         if ((eoi == -1) || (ess == -1)) {\n" + 
                     "            LOGGER.error(\"eoi and/or ess have invalid values: eoi == {} ess == {}\", eoi, ess);\n" + 
                     "            CTRLINST.terminateMonitoring();\n" + 
                     "         }\n" + 
                     "      }\n" + 
                     "      // measure before\n" + 
                     "      final long tin = TIME.getTime();");
               BlockStmt finallyBlock = new BlockStmt();
               finallyBlock.addAndGetStatement("// measure after\n" + 
                     "         final long tout = TIME.getTime();\n" + 
                     "         CTRLINST.newMonitoringRecord(new OperationExecutionRecord(signature, sessionId, traceId, tin, tout, hostname, eoi, ess));\n" + 
                     "         // cleanup\n" + 
                     "         if (entrypoint) {\n" + 
                     "            CFREGISTRY.unsetThreadLocalTraceId();\n" + 
                     "            CFREGISTRY.unsetThreadLocalEOI();\n" + 
                     "            CFREGISTRY.unsetThreadLocalESS();\n" + 
                     "         } else {\n" + 
                     "            CFREGISTRY.storeThreadLocalESS(ess); // next operation is ess\n" + 
                     "         }");
               TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
               replacedStatement.addAndGetStatement(stmt);
               
               method.setBody(replacedStatement);
               
            } else {
               LOG.info("Unable to instrument " + name + "." + method.getNameAsString() + " because it has no body");
            }
         }
      }
   }
}
