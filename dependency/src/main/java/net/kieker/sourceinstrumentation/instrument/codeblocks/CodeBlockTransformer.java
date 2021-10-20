package net.kieker.sourceinstrumentation.instrument.codeblocks;

import com.github.javaparser.ast.body.TypeDeclaration;

import net.kieker.sourceinstrumentation.InstrumentationConstants;

public class CodeBlockTransformer {

   private final TypeDeclaration<?> topLevelType;

   public CodeBlockTransformer(final TypeDeclaration<?> topLevelType) {
      this.topLevelType = topLevelType;
   }
   
   public String getControllerName(final boolean useStaticVariables) {
      final String controllerName;
      if (useStaticVariables) {
         controllerName = topLevelType.getNameAsString() + "." + InstrumentationConstants.PREFIX + "controller";
      } else {
         controllerName = InstrumentationConstants.CONTROLLER_NAME;
      }
      return controllerName;
   }

   public String getTransformedBlock(final String originalBlock, final boolean useStaticVariables) {
      String transformedBlock;
      if (useStaticVariables) {
         transformedBlock = replaceTypeVariables(originalBlock);
      } else {
         transformedBlock = replaceStaticVariables(originalBlock);
      }
      return transformedBlock;
   }

   public static String replaceStaticVariables(final String original) {
      String before = original
            .replaceAll(InstrumentationConstants.PREFIX + "VM_NAME", "kieker.monitoring.core.controller.MonitoringController.getInstance().getHostname()")
            .replaceAll(InstrumentationConstants.PREFIX + "SESSION_REGISTRY", "SessionRegistry.INSTANCE")
            .replaceAll(InstrumentationConstants.PREFIX + "controlFlowRegistry", "ControlFlowRegistry.INSTANCE")
            .replaceAll(InstrumentationConstants.PREFIX + "controller", InstrumentationConstants.CONTROLLER_NAME)
            .replaceAll(InstrumentationConstants.PREFIX + "TIME_SOURCE", "kieker.monitoring.core.controller.MonitoringController.getInstance().getTimeSource()");
      return before;
   }

   private String replaceTypeVariables(final String original) {
      String before = original
            .replaceAll(InstrumentationConstants.PREFIX + "VM_NAME", topLevelType.getNameAsString() + "." + InstrumentationConstants.PREFIX + "VM_NAME")
            .replaceAll(InstrumentationConstants.PREFIX + "SESSION_REGISTRY", topLevelType.getNameAsString() + "." + InstrumentationConstants.PREFIX + "SESSION_REGISTRY")
            .replaceAll(InstrumentationConstants.PREFIX + "controlFlowRegistry", topLevelType.getNameAsString() + "." + InstrumentationConstants.PREFIX + "controlFlowRegistry")
            .replaceAll(InstrumentationConstants.PREFIX + "controller", topLevelType.getNameAsString() + "." + InstrumentationConstants.PREFIX + "controller")
            .replaceAll(InstrumentationConstants.PREFIX + "TIME_SOURCE", topLevelType.getNameAsString() + "." + InstrumentationConstants.PREFIX + "TIME_SOURCE");
      return before;
   }

}
