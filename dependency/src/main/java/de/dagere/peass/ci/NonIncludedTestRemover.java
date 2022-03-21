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
         for (Iterator<Map.Entry<TestCase, Set<String>>> testcaseIterator = tests.getTestcases().entrySet().iterator(); testcaseIterator.hasNext();) {
            Map.Entry<TestCase, Set<String>> testcase = testcaseIterator.next();
            if (!testcase.getValue().isEmpty()) {
               removeTestsWithMethod(executionConfig, testcaseIterator, testcase);
            } else {
               removeTestsWithoutMethod(executionConfig, testcaseIterator, testcase);
            }
         }
      }

   }

   private static void removeTestsWithoutMethod(final ExecutionConfig executionConfig, final Iterator<Map.Entry<TestCase, Set<String>>> testcaseIterator,
         final Map.Entry<TestCase, Set<String>> testcase) {
      TestCase test = new TestCase(testcase.getKey().getClazz());
      if (!isTestIncluded(test, executionConfig)) {
         testcaseIterator.remove();
      }
   }

   private static void removeTestsWithMethod(final ExecutionConfig executionConfig, final Iterator<Map.Entry<TestCase, Set<String>>> testcaseIterator,
         final Map.Entry<TestCase, Set<String>> testcase) {
      for (Iterator<String> methodIterator = testcase.getValue().iterator(); methodIterator.hasNext();) {
         String method = methodIterator.next();
         if (!isTestIncluded(new TestCase(testcase.getKey().getClazz(), method), executionConfig)) {
            methodIterator.remove();
         }
      }
      if (testcase.getValue().size() == 0) {
         testcaseIterator.remove();
      }
   }

   public static void removeNotIncluded(final Set<TestCase> tests, final ExecutionConfig executionConfig) {
      if (executionConfig.getIncludes().size() > 0 || executionConfig.getExcludes().size() > 0) {
         for (Iterator<TestCase> it = tests.iterator(); it.hasNext();) {
            TestCase test = it.next();
            boolean isIncluded = isTestIncluded(test, executionConfig);
            if (!isIncluded) {
               LOG.info("Excluding non-included test {}", test);
               it.remove();
            }
         }
      }
   }

   public static boolean isTestClassIncluded(final TestCase test, final ExecutionConfig config) {
      List<String> includes = config.getIncludes();
      boolean isIncluded;
      if (includes.size() != 0) {
         isIncluded = false;
         for (String include : includes) {
            boolean match;
            if (include.contains("#")) {
               String includeWithoutHash = include.substring(0, include.indexOf('#'));
               match = testMatch(test, includeWithoutHash);
            } else {
               match = testMatch(test, include);
            }
            if (match) {
               isIncluded = true;
               break;
            }
         }
      } else {
         isIncluded = true;
      }
      return isIncluded;
   }

   public static boolean isTestIncluded(final TestCase test, final ExecutionConfig config) {
      List<String> includes = config.getIncludes();
      List<String> excludes = config.getExcludes();
      boolean isIncluded;
      if (includes.size() != 0) {
         isIncluded = false;
         for (String include : includes) {
            boolean match = testMatch(test, include);

            if (match) {
               isIncluded = true;
               break;
            }
         }
      } else {
         isIncluded = true;
      }
      if (excludes.size() > 0 && isIncluded) {
         for (String exclude : excludes) {
            boolean match = testMatch(test, exclude);
            if (match) {
               isIncluded = false;
            }
         }
      }

      return isIncluded;
   }

   private static boolean testMatch(final TestCase test, final String pattern) {
      boolean match;
      if (pattern.contains(ChangedEntity.MODULE_SEPARATOR)) {
         String mergedName = test.getModule() + ChangedEntity.MODULE_SEPARATOR + test.getExecutable();
         match = FilenameUtils.wildcardMatch(mergedName, pattern);
         LOG.trace("Testing {} {} {}", mergedName, pattern, match);
      } else {
         match = FilenameUtils.wildcardMatch(test.getExecutable(), pattern);
         LOG.trace("Testing {} {} {}", test.getExecutable(), pattern, match);
      }
      return match;
   }
}
