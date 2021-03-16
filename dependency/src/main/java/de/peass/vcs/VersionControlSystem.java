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
import java.io.IOException;

/**
 * Represents the used version control system
 * 
 * @author reichelt
 *
 */
public enum VersionControlSystem {
   SVN, GIT;

   private static boolean parentFolderHasVCS(final File folder, final String vcsFileName) {
      File potentialVCSFile = new File(folder, vcsFileName);
      if (potentialVCSFile.exists()) {
         return true;
      } else {
         File parentFile = folder.getParentFile();
         if (parentFile != null) {
            return parentFolderHasVCS(parentFile, vcsFileName);
         } else {
            return false;
         }
      }
   }
   
   public static File findVCSFolder(final File folder) {
      try {
         return findVCSFolder(folder.getCanonicalFile(), ".git");
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
   
   public static File findVCSFolder(final File folder, final String vcsFileName) {
      File potentialVCSFile = new File(folder, vcsFileName);
      if (potentialVCSFile.exists()) {
         return folder;
      } else {
         File parentFile = folder.getParentFile();
         if (parentFile != null) {
            return findVCSFolder(parentFile, vcsFileName);
         } else {
            return null;
         }
      }
   }

   public static VersionControlSystem getVersionControlSystem(final File projectFolder) {
      try {
         if (parentFolderHasVCS(projectFolder.getCanonicalFile(), ".svn")) {
            return SVN;
         } else if (parentFolderHasVCS(projectFolder.getCanonicalFile(), ".git")) {
            return GIT;
         } else {
            throw new RuntimeException("Unknown version control system type in " + projectFolder.getAbsolutePath() + " - .git and .svn not found");
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
