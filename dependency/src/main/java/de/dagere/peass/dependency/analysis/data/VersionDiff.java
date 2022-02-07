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
package de.dagere.peass.dependency.analysis.data;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;

/**
 * Holds data of the difference between two versions, i.e. which classes are changed and whether the pom has changed or not.
 * 
 * @author reichelt
 *
 */
public class VersionDiff {

   private static final Logger LOG = LogManager.getLogger(VersionDiff.class);

   private boolean pomChanged;
   private final List<ChangedEntity> changedClasses;
   private final List<File> modules;
   private final File projectFolder;

   public VersionDiff(final List<File> modules, final File projectFolder) {
      changedClasses = new LinkedList<>();
      pomChanged = false;
      this.modules = modules;
      this.projectFolder = projectFolder;
   }

   /**
    * @return the pomChanged
    */
   public boolean isPomChanged() {
      return pomChanged;
   }

   /**
    * @param pomChanged the pomChanged to set
    */
   public void setPomChanged(final boolean pomChanged) {
      this.pomChanged = pomChanged;
   }

   public List<ChangedEntity> getChangedClasses() {
      return changedClasses;
   }

   public void addChange(final String currentFileName, final ExecutionConfig config) {
      if (currentFileName.endsWith("pom.xml")) {
         setPomChanged(true);
      } else {
         if (currentFileName.endsWith(".java")) {
            String fileNameWithoutExtension = currentFileName.replace(".java", "");
            String containedPath = null;
            for (String path : config.getAllClazzFolders()) {
               if (fileNameWithoutExtension.contains(path)) {
                  containedPath = path;
                  break;
               }
            }

            final int indexOf = currentFileName.indexOf(containedPath);
            if (indexOf == -1) {
               LOG.error("Did not find any of the class pathes in the changed filename: {} classpathes: {} ", currentFileName, config.getAllClazzFolders());
            } else {
               addChange(currentFileName, containedPath, indexOf);
            }
         }
      }
   }

   private void addChange(final String currentFileName, final String containedPath, final int indexOf) {
      if (indexOf != 0) {
         final String pathWithFolder = currentFileName.substring(indexOf);
         final String classPath = replaceClazzFolderFromName(pathWithFolder, containedPath);
         final String modulePath = currentFileName.substring(0, indexOf - 1);
         final File moduleFile = new File(projectFolder, modulePath);
         if (modules.contains(moduleFile)) {
            LOG.debug("Create new changedentitty: " + classPath + " " + modulePath);
            final ChangedEntity changedEntity = new ChangedEntity(classPath, modulePath);
            if (!changedEntity.getJavaClazzName().contains(File.separator)) {
               changedClasses.add(changedEntity);
            } else {
               LOG.error("Sourcefoldernot found: {} Ignoring {}", modulePath, currentFileName);
            }
         } else {
            LOG.error("Unexpected Module: {} Ignoring {}", modulePath, currentFileName);
            LOG.info("Modules: {}", modules);
         }

      } else {
         final String classPath = replaceClazzFolderFromName(currentFileName, containedPath);
         final ChangedEntity changedEntity = new ChangedEntity(classPath, "");
         if (!changedEntity.getJavaClazzName().contains(File.separator)) {
            changedClasses.add(changedEntity);
         } else {
            LOG.error("Sourcefoldernot found: {}", currentFileName);
         }
      }
   }

   public static String replaceClazzFolderFromName(final String fileName, final String classFolderName) {
      String tempClazzName = fileName.replace(".java", "");
      tempClazzName = tempClazzName.replaceAll(classFolderName, "");
      if (tempClazzName.startsWith(File.separator) || tempClazzName.startsWith("/")) {
         tempClazzName = tempClazzName.substring(1);
      }
      String replaced = tempClazzName.replace(File.separatorChar, '.');
      if (replaced.contains("/")) {
         replaced = replaced.replace('/', '.'); // In case windows accidently used / as separator
      }
      return replaced;
   }

   @Override
   public String toString() {
      String ret = "Pom: " + pomChanged + " Klassen: ";
      for (final ChangedEntity cl : changedClasses) {
         if (cl.getModule().length() > 0) {
            ret += cl.getClazz() + "\n";
         } else {
            ret += cl.getModule() + "-" + cl.getClazz() + "\n";
         }
      }
      return ret;
   }
}