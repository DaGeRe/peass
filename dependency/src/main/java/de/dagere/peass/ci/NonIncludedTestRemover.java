package de.dagere.peass.ci;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;

public class NonIncludedTestRemover {

   private static final Logger LOG = LogManager.getLogger(NonIncludedTestRemover.class);

   public static void removeNotIncluded(final TestSet tests, final ExecutionConfig executionConfig) {
      if (executionConfig.getIncludes().size() > 0) {
         for (Iterator<Map.Entry<ChangedEntity, Set<String>>> testcaseIterator = tests.getTestcases().entrySet().iterator(); testcaseIterator.hasNext();) {
            Map.Entry<ChangedEntity, Set<String>> testcase = testcaseIterator.next();
            if (!testcase.getValue().isEmpty()) {
               removeTestsWithMethod(executionConfig, testcaseIterator, testcase);
            } else {
               removeTestsWithoutMethod(executionConfig, testcaseIterator, testcase);
            }
         }
      }

   }

   private static void removeTestsWithoutMethod(final ExecutionConfig executionConfig, Iterator<Map.Entry<ChangedEntity, Set<String>>> testcaseIterator,
         Map.Entry<ChangedEntity, Set<String>> testcase) {
      TestCase test = new TestCase(testcase.getKey().getJavaClazzName());
      if (!isTestIncluded(test, executionConfig.getIncludes())) {
         testcaseIterator.remove();
      }
   }

   private static void removeTestsWithMethod(final ExecutionConfig executionConfig, Iterator<Map.Entry<ChangedEntity, Set<String>>> testcaseIterator,
         Map.Entry<ChangedEntity, Set<String>> testcase) {
      for (Iterator<String> methodIterator = testcase.getValue().iterator(); methodIterator.hasNext();) {
         String method = methodIterator.next();
         if (!isTestIncluded(new TestCase(testcase.getKey().getJavaClazzName(), method), executionConfig.getIncludes())) {
            methodIterator.remove();
         }
      }
      if (testcase.getValue().size() == 0) {
         testcaseIterator.remove();
      }
   }

   public static void removeNotIncluded(final Set<TestCase> tests, final ExecutionConfig executionConfig) {
      if (executionConfig.getIncludes().size() > 0) {
         for (Iterator<TestCase> it = tests.iterator(); it.hasNext();) {
            TestCase test = it.next();
            boolean isIncluded = isTestIncluded(test, executionConfig.getIncludes());
            if (!isIncluded) {
               LOG.info("Excluding non-included test {}", test);
               it.remove();
            }
         }
      }
   }

   public static boolean isTestIncluded(final TestCase test, final List<String> includes) {
      if (includes.size() == 0) {
         return true;
      }
      boolean isIncluded = false;
      for (String include : includes) {
         boolean match = FilenameUtils.wildcardMatch(test.getExecutable(), include);
         LOG.info("Testing {} {} {}", test.getExecutable(), include, match);
         if (match) {
            isIncluded = true;
            break;
         }
      }
      return isIncluded;
   }
}
