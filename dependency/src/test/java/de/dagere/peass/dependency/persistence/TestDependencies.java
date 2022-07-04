package de.dagere.peass.dependency.persistence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.tools.xjc.addon.code_injector.Const;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.utils.Constants;

public class TestDependencies {
   
   @Test
   public void testWithVersions() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialcommit(new InitialCommit());
      dependencies.getInitialcommit().setCommit("0");
      dependencies.getVersions().put("1", new VersionStaticSelection());
      dependencies.getVersions().put("2", new VersionStaticSelection());
      dependencies.getVersions().put("3", new VersionStaticSelection());
      
      String[] versionNames = dependencies.getVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("1", versionNames[1]);
      Assert.assertEquals("2", versionNames[2]);
      Assert.assertEquals("3", versionNames[3]);
      
      Assert.assertEquals("3", dependencies.getNewestVersion());
   }
   
   @Test
   public void testOnlyStartversion() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialcommit(new InitialCommit());
      dependencies.getInitialcommit().setCommit("0");
      
      String[] versionNames = dependencies.getVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("0", dependencies.getNewestVersion());
   }
   
   @Test
   public void testRunningersions() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialcommit(new InitialCommit());
      dependencies.getInitialcommit().setCommit("0");
      VersionStaticSelection running1 = new VersionStaticSelection();
      running1.setRunning(true);
      dependencies.getVersions().put("1", running1);
      VersionStaticSelection nonRunning2 = new VersionStaticSelection();
      nonRunning2.setRunning(false);
      dependencies.getVersions().put("2", nonRunning2);
      VersionStaticSelection running3 = new VersionStaticSelection();
      running3.setRunning(true);
      dependencies.getVersions().put("3", running3);
      
      String[] versionNames = dependencies.getRunningVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("1", versionNames[1]);
      Assert.assertEquals("3", versionNames[2]);
      
      Assert.assertEquals("3", dependencies.getNewestVersion());
   }
   
   private static final String OLD_DATA_EXAMPLE = "{\n"
         + "  \"url\" : null,\n"
         + "  \"testGoal\" : null,\n"
         + "  \"initialversion\" : {\n"
         + "    \"version\" : \"asdasd\",\n"
         + "    \"jdk\" : 8,\n"
         + "    \"initialDependencies\" : {\n"
         + "      \"Test#A#methodA2\" : {\n"
         + "        \"entities\" : [ {\n"
         + "          \"clazz\" : \"Test#A\",\n"
         + "          \"method\" : \"methodA2\",\n"
         + "          \"parameters\" : [ ]\n"
         + "        } ]\n"
         + "      }\n"
         + "    }\n"
         + "  },\n"
         + "  \"versions\" : {\n"
         + "    \"bsdbsd\" : {\n"
         + "      \"running\" : false,\n"
         + "      \"jdk\" : 8,\n"
         + "      \"changedClazzes\" : { }\n"
         + "    }\n"
         + "  },\n"
         + "  \"android\" : false\n"
         + "}";
   
   @Test
   public void testSerialization() throws JsonProcessingException {
      StaticTestSelection selection = new StaticTestSelection();
      selection.getInitialcommit().setCommit("asdasd");
      selection.getInitialcommit().addDependency(new TestCase("Test#A#methodA2"), new ChangedEntity("Test#A#methodA2"));
      selection.getVersions().put("bsdbsd", new VersionStaticSelection());
      selection.getVersions().get("bsdbsd").setRunning(false);
      
      String serialized = Constants.OBJECTMAPPER.writeValueAsString(selection);
      System.out.println(serialized);
      
      StaticTestSelection deserialized = Constants.OBJECTMAPPER.readValue(serialized, StaticTestSelection.class);
      Assert.assertEquals(deserialized.getInitialcommit().getCommit(), "asdasd");
      
      StaticTestSelection deserializedOldData = Constants.OBJECTMAPPER.readValue(OLD_DATA_EXAMPLE, StaticTestSelection.class);
      Assert.assertEquals(deserializedOldData.getInitialcommit().getCommit(), "asdasd");
      
   }
}
