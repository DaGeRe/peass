package de.dagere.peass.dependency.persistence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.utils.Constants;

public class TestStaticTestSelection {
   
   @Test
   public void testWithVersions() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialcommit(new InitialCommit());
      dependencies.getInitialcommit().setCommit("0");
      dependencies.getCommits().put("1", new CommitStaticSelection());
      dependencies.getCommits().put("2", new CommitStaticSelection());
      dependencies.getCommits().put("3", new CommitStaticSelection());
      
      String[] versionNames = dependencies.getCommitNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("1", versionNames[1]);
      Assert.assertEquals("2", versionNames[2]);
      Assert.assertEquals("3", versionNames[3]);
      
      Assert.assertEquals("3", dependencies.getNewestCommit());
   }
   
   @Test
   public void testOnlyStartversion() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialcommit(new InitialCommit());
      dependencies.getInitialcommit().setCommit("0");
      
      String[] versionNames = dependencies.getCommitNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("0", dependencies.getNewestCommit());
   }
   
   @Test
   public void testRunningersions() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialcommit(new InitialCommit());
      dependencies.getInitialcommit().setCommit("0");
      CommitStaticSelection running1 = new CommitStaticSelection();
      running1.setRunning(true);
      dependencies.getCommits().put("1", running1);
      CommitStaticSelection nonRunning2 = new CommitStaticSelection();
      nonRunning2.setRunning(false);
      dependencies.getCommits().put("2", nonRunning2);
      CommitStaticSelection running3 = new CommitStaticSelection();
      running3.setRunning(true);
      dependencies.getCommits().put("3", running3);
      
      String[] versionNames = dependencies.getRunningCommitNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("1", versionNames[1]);
      Assert.assertEquals("3", versionNames[2]);
      
      Assert.assertEquals("3", dependencies.getNewestCommit());
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
      selection.getCommits().put("bsdbsd", new CommitStaticSelection());
      selection.getCommits().get("bsdbsd").setRunning(false);
      
      String serialized = Constants.OBJECTMAPPER.writeValueAsString(selection);
      System.out.println(serialized);
      
      StaticTestSelection deserialized = Constants.OBJECTMAPPER.readValue(serialized, StaticTestSelection.class);
      Assert.assertEquals(deserialized.getInitialcommit().getCommit(), "asdasd");
      
      StaticTestSelection deserializedOldData = Constants.OBJECTMAPPER.readValue(OLD_DATA_EXAMPLE, StaticTestSelection.class);
      Assert.assertEquals(deserializedOldData.getInitialcommit().getCommit(), "asdasd");
      Assert.assertNull(deserializedOldData.getInitialversion());
      
   }
}
