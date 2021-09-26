package net.kieker.sourceinstrumentation.instrument;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.Type;

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

      
      addField(clazz, keywords, 0,
            "kieker.monitoring.core.controller.IMonitoringController", 
            InstrumentationConstants.PREFIX + "controller", 
            new MethodCallExpr("kieker.monitoring.core.controller.MonitoringController.getInstance"));
      
      addField(clazz, keywords, 1,
            "kieker.monitoring.timer.ITimeSource", 
            InstrumentationConstants.PREFIX + "TIME_SOURCE", 
            new MethodCallExpr(InstrumentationConstants.PREFIX + "controller.getTimeSource"));

      if (configuration.getUsedRecord() == AllowedKiekerRecord.OPERATIONEXECUTION) {
         addMetadataFields(clazz, keywords);
      }
   }

   private void addMetadataFields(final TypeDeclaration<?> clazz, final Keyword[] keywords) {
      addField(clazz, keywords, 2,
            "String", 
            InstrumentationConstants.PREFIX + "VM_NAME",
            new MethodCallExpr(InstrumentationConstants.PREFIX + "controller.getHostname"));

      addField(clazz, keywords, 3, 
            "kieker.monitoring.core.registry.SessionRegistry", 
            InstrumentationConstants.PREFIX + "SESSION_REGISTRY",
            new FieldAccessExpr(new NameExpr("SessionRegistry"), "INSTANCE"));

      addField(clazz, keywords, 4, 
            "kieker.monitoring.core.registry.ControlFlowRegistry", 
            InstrumentationConstants.PREFIX + "controlFlowRegistry",
            new FieldAccessExpr(new NameExpr("ControlFlowRegistry"), "INSTANCE"));
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
   
   private void addField(final TypeDeclaration<?> clazz, final Keyword[] keywords, final int position, final String typeName, final String variableName, final Expression initExpression) {
      Type type = StaticJavaParser.parseType(typeName);
      
      FieldDeclaration fieldDeclaration = new FieldDeclaration();
      
      VariableDeclarator variable = new VariableDeclarator(type, variableName);
      fieldDeclaration.getVariables().add(variable);
      fieldDeclaration.setModifiers(Modifier.createModifierList(keywords));
      clazz.getMembers().add(position, fieldDeclaration);
      
      fieldDeclaration.getVariables().iterator().next().setInitializer(initExpression);
   }
}
