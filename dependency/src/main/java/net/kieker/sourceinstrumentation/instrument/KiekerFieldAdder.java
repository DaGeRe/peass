package net.kieker.sourceinstrumentation.instrument;

import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.InstrumentationConstants;

public class KiekerFieldAdder {
   
   private final InstrumentationConfiguration configuration;
   
   public KiekerFieldAdder(final InstrumentationConfiguration configuration) {
      this.configuration = configuration;
   }

   public void addKiekerFields(final TypeDeclaration<?> clazz) {
      Keyword[] keywords = getKeywords(clazz);

      clazz.addFieldWithInitializer("kieker.monitoring.core.controller.IMonitoringController", InstrumentationConstants.PREFIX + "controller",
            new MethodCallExpr("kieker.monitoring.core.controller.MonitoringController.getInstance"),
            keywords);

      clazz.addFieldWithInitializer("kieker.monitoring.timer.ITimeSource", InstrumentationConstants.PREFIX + "TIME_SOURCE",
            new MethodCallExpr(InstrumentationConstants.PREFIX + "controller.getTimeSource"),
            keywords);

      if (configuration.getUsedRecord() == AllowedKiekerRecord.OPERATIONEXECUTION) {
         addMetadataFields(clazz, keywords);
      }
   }

   private void addMetadataFields(final TypeDeclaration<?> clazz, Keyword[] keywords) {
      clazz.addFieldWithInitializer("String", InstrumentationConstants.PREFIX + "VM_NAME",
            new MethodCallExpr(InstrumentationConstants.PREFIX + "controller.getHostname"),
            keywords);

      clazz.addFieldWithInitializer("kieker.monitoring.core.registry.SessionRegistry", InstrumentationConstants.PREFIX + "SESSION_REGISTRY",
            new FieldAccessExpr(new NameExpr("SessionRegistry"), "INSTANCE"),
            keywords);

      clazz.addFieldWithInitializer("kieker.monitoring.core.registry.ControlFlowRegistry", InstrumentationConstants.PREFIX + "controlFlowRegistry",
            new FieldAccessExpr(new NameExpr("ControlFlowRegistry"), "INSTANCE"),
            keywords);
   }

   private Keyword[] getKeywords(final TypeDeclaration<?> clazz) {
      Keyword[] keywords;
      if (clazz instanceof ClassOrInterfaceDeclaration) {
         ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) clazz;
         if (declaration.isInterface()) {
            keywords = new Keyword[] { Keyword.STATIC, Keyword.FINAL };
         } else {
            keywords = new Keyword[] { Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL };
         }
      } else {
         keywords = new Keyword[] { Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL };
      }
      return keywords;
   }
}
