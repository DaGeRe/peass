package de.dagere.peass.dependency.analysis.data;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class TestTestCase {
   @Test
   public void testPackageGetting() {
      String packageName = new TestMethodCall("my.package.Clazz", "method").getPackage();
      Assert.assertEquals("my.package", packageName);
   }
}
