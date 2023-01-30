/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.vcs;

import java.io.File;
import java.util.List;

import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.CommitDiff;
import picocli.CommandLine.Mixin;

/**
 * Allows iteration over git-versions
 * 
 * @author reichelt
 *
 */
public class CommitIteratorGit extends CommitIterator {

   private static final Logger LOG = LogManager.getLogger(CommitIteratorGit.class);

   private final List<String> commits;
   private final String previous;
   private final int previousIndex;

   @Mixin
   private ExecutionConfigMixin executionConfigMixin;

   public CommitIteratorGit(final File projectFolder) {
      super(projectFolder);
      previous = GitUtils.getName("HEAD~1", projectFolder);
      commits = GitUtils.getCommits(projectFolder, false, executionConfigMixin.isLinearizeHistory());
      previousIndex = commits.indexOf(previous);
   }
   
   /**
    * Initializes the iterator with a project, which is changed when the iterator moves, a list of commits and the previous commit for starting.
    * 
    * @param projectFolder Folder, where versions should be checked out
    * @param commits List of commits
    * @param previousCommit Previous commit before start (NO_BEFORE, if it is the first one)
    */
   public CommitIteratorGit(final File projectFolder, final List<String> commits, final String previousCommit) {
      super(projectFolder);
      this.commits = commits;
      this.previous = previousCommit;
      int index = -1;
      if (previousCommit != null) {
         for (int i = 0; i < commits.size(); i++) {
            String testedCommit = commits.get(i);
            LOG.debug("Trying " + testedCommit);
            if (testedCommit.equals(previousCommit)) {
               LOG.debug("{} equals {}, setting start index to {}", testedCommit, previousCommit, i);
               index = i;
            }
         }
      }
      previousIndex = index;
      commitIndex = previousIndex + 1;
   }

   @Override
   public boolean goToFirstCommit() {
      commitIndex = 0;
      GitUtils.goToCommit(commits.get(0), projectFolder);
      return true;
   }

   @Override
   public boolean goToNextCommit() {
      commitIndex++;
      final String nextTag = commits.get(commitIndex);
      GitUtils.goToCommit(nextTag, projectFolder);
      return true;
   }

   @Override
   public boolean goToPreviousCommit() {
      if (commitIndex > 0) {
         commitIndex--;
         final String nextTag = commits.get(commitIndex);
         GitUtils.goToCommit(nextTag, projectFolder);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean hasNextCommit() {
      return commitIndex + 1 < commits.size();
   }
   
   @Override
   public String getCommitName() {
      return commits.get(commitIndex);
   }
   
   @Override
   public String getPredecessor() {
      return previous;
   }

   @Override
   public int getSize() {
      return commits.size();
   }
   
   @Override
   public int getRemainingSize() {
      return commits.size() - commitIndex;
   }

   @Override
   public boolean goTo0thCommit() {
      if (previousIndex != -1) {
         GitUtils.goToCommit(previous, projectFolder);
         commitIndex = previousIndex;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isPredecessor(final String lastRunningCommit) {
      return commits.get(commitIndex - 1).equals(lastRunningCommit);
   }
   
   @Override
   public CommitDiff getChangedClasses(final File projectFolder, final List<File> genericModules, final String lastCommit, final ExecutionConfig config) {
      CommitDiff diff = GitUtils.getChangedClasses(projectFolder, genericModules, lastCommit, config);
      return diff;
   }
   
   @Override
   public List<String> getCommits() {
      return commits;
   }
}
