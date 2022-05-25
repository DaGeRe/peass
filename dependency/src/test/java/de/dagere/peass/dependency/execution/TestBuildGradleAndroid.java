package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


@Disabled("This tests are only executable with ANDROID_SDK_ROOT, therefore, they are disabled by default")
public class TestBuildGradleAndroid extends TestBuildGradle {
   @Test
   public void testBuildtoolUpdate() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "build.gradle");
      testUpdate(gradleFile, true);

      final File gradleFile2 = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "v2.gradle");
      testUpdate(gradleFile2, true);
   }

   @Test
   public void testAndroidLib() throws IOException {
      final File gradleFile3 = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");
      testUpdate(gradleFile3, false);
   }

}
