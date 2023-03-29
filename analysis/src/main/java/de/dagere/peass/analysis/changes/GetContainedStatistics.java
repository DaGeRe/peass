package de.dagere.peass.analysis.changes;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.nodeDiffGenerator.data.TestMethodCall;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.utils.Constants;

public class GetContainedStatistics {
   public static void main(String[] args) throws StreamReadException, DatabindException, IOException {
      File changeFile = new File(args[0]);
      File executionFile = new File(args[1]);
      
      ProjectChanges projectChanges = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);
      ExecutionData executions = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
      
      
      int selected = 0, changed = 0;
      int notSelected = 0;
      for (Entry<String, Changes> commitEntry : projectChanges.getCommitChanges().entrySet()) {
         Set<TestMethodCall> currentCommitsSelectedTests = executions.getCommits().get(commitEntry.getKey()).getTestMethods();
         int currentCommitChangedTests = commitEntry.getValue().getTests().getTestMethods().size();
         changed+= currentCommitChangedTests;
         for (TestMethodCall tet : commitEntry.getValue().getTests().getTestMethods()) {
            if (!currentCommitsSelectedTests.contains(tet)) {
               System.out.println("Wrong: " + commitEntry.getKey() + " " + tet);
               notSelected++;
            }
         }
         selected += currentCommitsSelectedTests.size();
      }
      System.out.println("Not selected but changed: " + notSelected);
      System.out.println("Selected: " + selected + " Changed: " + changed);
   }
}
