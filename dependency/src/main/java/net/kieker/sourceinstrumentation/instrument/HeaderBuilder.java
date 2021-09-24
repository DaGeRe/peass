package net.kieker.sourceinstrumentation.instrument;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import net.kieker.sourceinstrumentation.InstrumentationConstants;

public class HeaderBuilder {
   
   private final boolean useStaticVariables, enableDeactivation, enableAdaptiveMonitoring;
   
   public HeaderBuilder(final boolean useStaticVariables, final boolean enableDeactivation, final boolean enableAdaptiveMonitoring) {
      this.useStaticVariables = useStaticVariables;
      this.enableDeactivation = enableDeactivation;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
   }

   public void buildHeader(final BlockStmt originalBlock, final String signature, final boolean needsReturn, final BlockStmt replacedStatement) {
      boolean afterUnreachable = ReachabilityDecider.isAfterUnreachable(originalBlock);

      boolean addReturn = needsReturn && !afterUnreachable;
      BlockStmt changed = addReturn ? originalBlock.addStatement("return;") : originalBlock;

      final String controllerName = getControllerName();

      if (enableDeactivation) {
         addDeactivationStatement(replacedStatement, changed, controllerName);
      }
      replacedStatement.addAndGetStatement("final String " + InstrumentationConstants.PREFIX + "signature = \"" + signature + "\";");
      if (enableAdaptiveMonitoring) {
         addAdaptiveMonitoringStatement(replacedStatement, changed, controllerName);
      }
   }

   private void addAdaptiveMonitoringStatement(final BlockStmt replacedStatement, BlockStmt changed, final String controllerName) {
      NameExpr name = new NameExpr(InstrumentationConstants.PREFIX + "signature");
      Expression expr = new MethodCallExpr("!" + controllerName + ".isProbeActivated", name);
      IfStmt ifS = new IfStmt(expr, changed, null);
      replacedStatement.addStatement(ifS);
   }

   private void addDeactivationStatement(final BlockStmt replacedStatement, BlockStmt changed, final String controllerName) {
      Expression expr = new MethodCallExpr("!" + controllerName + ".isMonitoringEnabled");
      IfStmt ifS = new IfStmt(expr, changed, null);
      replacedStatement.addStatement(ifS);
   }

   private String getControllerName() {
      final String controllerName;
      if (useStaticVariables) {
         controllerName = InstrumentationConstants.PREFIX + "controller";
      } else {
         controllerName = InstrumentationConstants.CONTROLLER_NAME;
      }
      return controllerName;
   }
}
