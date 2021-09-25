package de.dagere.peass.dependency.execution.pom;

import java.io.File;

import org.junit.jupiter.api.Test;

public class TestSnapshotRemoveUtil {
   @Test
   public void testRegularRemoval() {
      File pomFile = new File("src/test/resources/example-snapshot-removal-pom/pom.xml");
      SnapshotRemoveUtil.cleanSnapshotDependencies(pomFile);
   }
}
