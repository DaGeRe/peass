package de.dagere.peass.execution.gradle;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.testtransformation.TestTransformer;

public class GradleBuildfileEditorAnbox {

   private final String COMPILE_SDK_VERSION = "    compileSdkVersion ";
   private final String MIN_SDK_VERSION = "        minSdkVersion ";
   private final String TARGET_SDK_VERSION = "        targetSdkVersion ";
   private final String MULTIDEX_ENABLED = "        multiDexEnabled = true";

   private final TestTransformer testTransformer;

   public GradleBuildfileEditorAnbox(TestTransformer testTransformer) {
      this.testTransformer = testTransformer;
   }

   public void executeAnboxSpecificTransformations(final GradleBuildfileVisitor visitor) {
      adaptSdkVersions(visitor);

      if (visitor.getMultiDexEnabled() != -1) {
         final int lineIndex = visitor.getMultiDexEnabled() - 1;
         visitor.getLines().set(lineIndex, MULTIDEX_ENABLED);
      } else {
         addLineWithinDefaultConfig(visitor, MULTIDEX_ENABLED);
      }

      addAndroidPackagingOptions(visitor);
      addJavaVersionCompatibilityOptions(visitor);
   }

   private void adaptSdkVersions(final GradleBuildfileVisitor visitor) {
      ExecutionConfig executionConfig = testTransformer.getConfig().getExecutionConfig();

      if (executionConfig.getAndroidCompileSdkVersion() != null && visitor.getCompileSdkVersion() != -1) {
         // assumption: compileSdkVersion always exists
         String versionText = COMPILE_SDK_VERSION + executionConfig.getAndroidCompileSdkVersion();
         final int lineIndex = visitor.getCompileSdkVersion() - 1;
         visitor.getLines().set(lineIndex, versionText);
      }

      if (executionConfig.getAndroidMinSdkVersion() != null) {
         String versionText = MIN_SDK_VERSION + executionConfig.getAndroidMinSdkVersion();

         if (visitor.getMinSdkVersion() != -1) {
            final int lineIndex = visitor.getMinSdkVersion() - 1;
            visitor.getLines().set(lineIndex, versionText);
         } else {
            addLineWithinDefaultConfig(visitor, versionText);
         }
      }

      if (executionConfig.getAndroidTargetSdkVersion() != null) {
         String versionText = TARGET_SDK_VERSION + executionConfig.getAndroidTargetSdkVersion();

         if (visitor.getTargetSdkVersion() != -1) {
            final int lineIndex = visitor.getTargetSdkVersion() - 1;
            visitor.getLines().set(lineIndex, versionText);
         } else {
            addLineWithinDefaultConfig(visitor, versionText);
         }
      }
   }

   private void addAndroidPackagingOptions(final GradleBuildfileVisitor visitor) {
      String[] excludeFiles = {
            "'META-INF/DEPENDENCIES'",
            "'META-INF/LICENSE.md'",
            "'META-INF/NOTICE.md'",
            "'META-INF/jing-copying.html'",
            "'META-INF/LICENSE-notice.md'",
      };
      if (visitor.getAndroidPackagingOptions() != -1) {
         addExcludeFiles(visitor, excludeFiles);
      } else {
         visitor.addLine(visitor.getAndroidLine() - 1, "    android.packagingOptions {");
         addExcludeFiles(visitor, excludeFiles);
         int androidPackagingOptionsEnd = visitor.getAndroidLine();
         visitor.addLine(androidPackagingOptionsEnd - 1, "    }");

         visitor.setAndroidPackagingOptions(androidPackagingOptionsEnd);
      }
   }

   private void addExcludeFiles(final GradleBuildfileVisitor visitor, String[] excludeFiles) {
      for (String file : excludeFiles) {
         String packagingOption = "        exclude " + file;
         visitor.addLine(visitor.getAndroidLine() - 1, packagingOption);
      }
   }

   private void addJavaVersionCompatibilityOptions(final GradleBuildfileVisitor visitor) {
      final String SOURCE_COMPATIBILITY = "sourceCompatibility JavaVersion.VERSION_1_8";
      final String TARGET_COMPATIBILITY = "targetCompatibility JavaVersion.VERSION_1_8";

      if (visitor.getSourceCompatibilityLine() != -1) {
         final int lineIndex = visitor.getSourceCompatibilityLine() - 1;
         visitor.getLines().set(lineIndex, SOURCE_COMPATIBILITY);
      } else {
         addLineWithinCompileOptions(visitor, SOURCE_COMPATIBILITY);
      }

      if (visitor.getTargetCompatibilityLine() != -1) {
         final int lineIndex = visitor.getTargetCompatibilityLine() - 1;
         visitor.getLines().set(lineIndex, TARGET_COMPATIBILITY);
      } else {
         addLineWithinCompileOptions(visitor, TARGET_COMPATIBILITY);
      }
   }

   private void addLineWithinCompileOptions(GradleBuildfileVisitor visitor, String textForAdding) {
      if (visitor.getCompileOptionsLine() != -1) {
         visitor.addLine(visitor.getCompileOptionsLine() - 1, textForAdding);
      } else {
         visitor.addLine(visitor.getAndroidLine() - 1, "    compileOptions {");
         visitor.addLine(visitor.getAndroidLine() - 1, textForAdding);

         int compileOptionsEnd = visitor.getAndroidLine();
         visitor.addLine(compileOptionsEnd - 1, "    }");

         visitor.setCompileOptionsLine(compileOptionsEnd);
      }
   }

   private void addLineWithinDefaultConfig(GradleBuildfileVisitor visitor, String textForAdding) {
      if (visitor.getDefaultConfigLine() != -1) {
         visitor.addLine(visitor.getDefaultConfigLine() - 1, textForAdding);
      } else {
         visitor.addLine(visitor.getAndroidLine() - 1, "    defaultConfig {");
         visitor.addLine(visitor.getAndroidLine() - 1, textForAdding);

         int defaultConfigEnd = visitor.getAndroidLine();
         visitor.addLine(defaultConfigEnd - 1, "    }");

         visitor.setDefaultConfigLine(defaultConfigEnd);
      }
   }
}
