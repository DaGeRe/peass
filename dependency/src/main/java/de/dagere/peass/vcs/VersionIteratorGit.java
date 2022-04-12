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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.VersionDiff;

/**
 * Allows iteration over git-versions
 * 
 * @author reichelt
 *
 */
public class VersionIteratorGit extends VersionIterator {

   private static final Logger LOG = LogManager.getLogger(VersionIteratorGit.class);

   private final List<GitCommit> entries;
   private final GitCommit previous;
   private final int previousIndex;

   public VersionIteratorGit(final File projectFolder) {
      super(projectFolder);
      previous = new GitCommit(GitUtils.getName("HEAD~1", projectFolder), "", "", "");
      entries = GitUtils.getCommits(projectFolder, false);
      previousIndex = entries.indexOf(previous);
   }
   
   /**
    * Initializes the iterator with a project, which is changed when the iterator moves, a list of commits and the previous commit for starting.
    * 
    * @param projectFolder Folder, where versions should be checked out
    * @param entries List of commits
    * @param previousCommit Previous commit before start (NO_BEFORE, if it is the first one)
    */
   public VersionIteratorGit(final File projectFolder, final List<GitCommit> entries, final GitCommit previousCommit) {
      super(projectFolder);
      this.entries = entries;
      this.previous = previousCommit;
      int index = -1;
      if (previousCommit != null) {
         for (int i = 0; i < entries.size(); i++) {
            String testedTag = entries.get(i).getTag();
            LOG.debug("Trying " + testedTag);
            if (testedTag.equals(previousCommit.getTag())) {
               LOG.debug("{} equals {}, setting start index to {}", testedTag, previousCommit.getTag(), i);
               index = i;
            }
         }
      }
      previousIndex = index;
      tagid = previousIndex + 1;
   }

   @Override
   public boolean goToFirstCommit() {
      tagid = 0;
      GitUtils.goToTag(entries.get(0).getTag(), projectFolder);
      return true;
   }

   @Override
   public boolean goToNextCommit() {
      tagid++;
      final String nextTag = entries.get(tagid).getTag();
      GitUtils.goToTag(nextTag, projectFolder);
      return true;
   }

   @Override
   public boolean goToPreviousCommit() {
      if (tagid > 0) {
         tagid--;
         final String nextTag = entries.get(tagid).getTag();
         GitUtils.goToTag(nextTag, projectFolder);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean hasNextCommit() {
      return tagid + 1 < entries.size();
   }

   @Override
   public String getTag() {
      return entries.get(tagid).getTag();
   }
   
   @Override
   public String getPredecessor() {
      return previous.getTag();
   }
   
   public GitCommit getPrevious() {
      return previous;
   }

   @Override
   public int getSize() {
      return entries.size();
   }
   
   @Override
   public int getRemainingSize() {
      return entries.size() - tagid;
   }

   @Override
   public boolean goTo0thCommit() {
      if (previousIndex != -1) {
         GitUtils.goToTag(previous.getTag(), projectFolder);
         tagid = previousIndex;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isPredecessor(final String lastRunningVersion) {
      return entries.get(tagid - 1).getTag().equals(lastRunningVersion);
   }
   
   @Override
   public VersionDiff getChangedClasses(final File projectFolder, final List<File> genericModules, final String lastVersion, final ExecutionConfig config) {
      VersionDiff diff = GitUtils.getChangedClasses(projectFolder, genericModules, lastVersion, config);
      return diff;
   }

}
