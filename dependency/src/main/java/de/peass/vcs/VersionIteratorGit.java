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
package de.peass.vcs;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Allows iteration over git-versions
 * 
 * @author reichelt
 *
 */
public class VersionIteratorGit extends VersionIterator {

   private static final Logger LOG = LogManager.getLogger(GitUtils.class);

   private final List<GitCommit> entries;
   private final GitCommit previous;
   private final int previousIndex;

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
            if (entries.get(i).getTag().equals(previousCommit.getTag())) {
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
   public boolean goToNextCommitSoft() {
      tagid++;
      final String nextTag = entries.get(tagid).getTag();
      GitUtils.goToTagSoft(nextTag, projectFolder);
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
   public int getSize() {
      return entries.size();
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
   public boolean isPredecessor(String lastRunningVersion) {
      return entries.get(tagid - 1).getTag().equals(lastRunningVersion);
   }

}
