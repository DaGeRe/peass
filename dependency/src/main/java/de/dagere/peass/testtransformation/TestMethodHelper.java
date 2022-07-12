package de.dagere.peass.testtransformation;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;

public class TestMethodHelper {

   private static final Logger LOG = LogManager.getLogger(TestMethodHelper.class);

   private final MeasurementConfig config;
   private final DataCollectorList datacollectorlist;

   public TestMethodHelper(final MeasurementConfig config, final DataCollectorList datacollectorlist) {
      this.config = config;
      this.datacollectorlist = datacollectorlist;
   }

   public void prepareTestMethods(final List<MethodDeclaration> testMethods) {
      for (MethodDeclaration testMethod : testMethods) {
         setPublic(testMethod);
         addAnnotation(testMethod);
      }
   }

   private void setPublic(final MethodDeclaration method) {
      if (!method.isPublic()) {
         method.setPublic(true);
         method.setPrivate(false);
         method.setProtected(false);
         method.setDefault(false);
      }
   }

   public void addAnnotation(final MethodDeclaration method) {
      for (final AnnotationExpr annotation : method.getAnnotations()) {
         if (annotation.getNameAsString().contains("PerformanceTest")) {
            LOG.info("Found annotation " + annotation.getNameAsString() + " - do not add annotation");
            return;
         }
      }

      final NormalAnnotationExpr performanceTestAnnotation = new NormalAnnotationExpr();
      performanceTestAnnotation.setName("de.dagere.kopeme.annotations.PerformanceTest");
      performanceTestAnnotation.addPair("iterations", "" + config.getAllIterations());
      performanceTestAnnotation.addPair("warmup", "" + 0);
      performanceTestAnnotation.addPair("executeBeforeClassInMeasurement", "" + config.getExecutionConfig().isExecuteBeforeClassInMeasurement());
      performanceTestAnnotation.addPair("logFullData", "" + true);
      performanceTestAnnotation.addPair("useKieker", "" + config.getKiekerConfig().isUseKieker());
      performanceTestAnnotation.addPair("timeout", "" + config.getExecutionConfig().getTimeout());
      performanceTestAnnotation.addPair("repetitions", "" + config.getRepetitions());
      performanceTestAnnotation.addPair("redirectToNull", "" + config.getExecutionConfig().isRedirectToNull());
      performanceTestAnnotation.addPair("showStart", "" + config.getExecutionConfig().isShowStart());
      if (config.getKiekerConfig().getKiekerWaitTime() != KiekerConfig.DEFAULT_KIEKER_WAIT_TIME) {
         performanceTestAnnotation.addPair("kiekerWaitTime", "" + config.getKiekerConfig().getKiekerWaitTime());
      }
      if (datacollectorlist.equals(DataCollectorList.ONLYTIME)) {
         performanceTestAnnotation.addPair("dataCollectors", "\"ONLYTIME\"");
      } else if (datacollectorlist.equals(DataCollectorList.ONLYTIME_NOGC)) {
         performanceTestAnnotation.addPair("dataCollectors", "\"ONLYTIME_NOGC\"");
      } else if (datacollectorlist.equals(DataCollectorList.NONE)) {
         performanceTestAnnotation.addPair("dataCollectors", "\"NONE\"");
      }
      method.addAnnotation(performanceTestAnnotation);
   }
}
