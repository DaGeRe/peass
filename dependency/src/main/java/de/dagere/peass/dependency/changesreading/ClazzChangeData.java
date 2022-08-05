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
package de.dagere.peass.dependency.changesreading;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

/**
 * Represents the relevant data of changes between two versions, i.e. whether there was a change, whether the change only affected methods, and if so, which methods where affected.
 * 
 * @author reichelt
 *
 */
public class ClazzChangeData {
   private static final Logger LOG = LogManager.getLogger(ClazzChangeData.class);
   
   private boolean isChange = false;
   private boolean isOnlyMethodChange = true;
   private final Map<String, Set<String>> changedMethods = new HashMap<>();
   private final Set<ChangedEntity> importChanges = new HashSet<>();
   private ChangedEntity containingFile;

   public ClazzChangeData(final ChangedEntity containingFile) {
      this.containingFile = containingFile;
   }

   public ClazzChangeData(final ChangedEntity containingFile, final boolean isOnlyMethodChange) {
      this.containingFile = containingFile;
      changedMethods.put(containingFile.getSimpleClazzName(), null);
      this.isOnlyMethodChange = isOnlyMethodChange;
   }

   public ClazzChangeData(final String clazz, final boolean isOnlyMethodChange) {
      this(new ChangedEntity(clazz, ""), isOnlyMethodChange);
      if (clazz.contains(ChangedEntity.CLAZZ_SEPARATOR)) {
         throw new RuntimeException("Class " + clazz + " should not contain module separator; use ChangedEntity constructor instead!");
      }
   }

   public ClazzChangeData(final String clazz, final String method) {
      addChange(clazz.substring(clazz.lastIndexOf('.') + 1), method);
      containingFile = new ChangedEntity(clazz, "");
      
      if (clazz.contains(ChangedEntity.CLAZZ_SEPARATOR)) {
         throw new RuntimeException("Class " + clazz + " should not contain module separator; use ChangedEntity constructor instead!");
      }
   }

   public boolean isChange() {
      return isChange;
   }

   public void setChange(final boolean isChange) {
      this.isChange = isChange;
   }

   public boolean isOnlyMethodChange() {
      return isOnlyMethodChange;
   }

   public void setOnlyMethodChange(final boolean isOnlyMethodChange) {
      this.isOnlyMethodChange = isOnlyMethodChange;
   }

   public Map<String, Set<String>> getChangedMethods() {
      return changedMethods;
   }

   @Override
   public String toString() {
      return "clazz: " + changedMethods.keySet() + " " + isChange + " " + isOnlyMethodChange + " " + changedMethods.values();
   }

   public void addChange(final String clazzWithoutPackage, final String method) {
      if (clazzWithoutPackage.contains(".")) {
         throw new RuntimeException("Clazz " + clazzWithoutPackage + " must not contain package!");
      }
      if (clazzWithoutPackage.equals("")) {
         throw new RuntimeException("Changed clazz must not be empty!");
      }
      isChange = true;
      Set<String> methods = changedMethods.get(clazzWithoutPackage);
      if (methods == null) {
         methods = new HashSet<>();
         changedMethods.put(clazzWithoutPackage, methods);
      }
      methods.add(method);
   }

   public void addClazzChange(final String clazzWithoutPackage) {
      if (clazzWithoutPackage.contains(".")) {
         throw new RuntimeException("Clazz " + clazzWithoutPackage + " must not contain package!");
      }
      if (clazzWithoutPackage.equals("")) {
         throw new RuntimeException("Changed clazz must not be empty!");
      }
      if (!changedMethods.containsKey(clazzWithoutPackage)) {
         changedMethods.put(clazzWithoutPackage, null);
      }
      // changedMethods.put(clazz, null);
      isChange = true;
      isOnlyMethodChange = false;
   }

   public void addClazzChange(final ChangedEntity clazz) {
      addClazzChange(clazz.getSimpleClazzName());
   }

   @JsonIgnore
   public Set<ChangedEntity> getUniqueChanges() {
      Set<ChangedEntity> entities = new HashSet<>();
      for (Map.Entry<String, Set<String>> change : changedMethods.entrySet()) {
         String fullQualifiedClassname = getFQN(change.getKey());
         if (isOnlyMethodChange) {
            for (String method : change.getValue()) {
               ChangedEntity entitity = new ChangedEntity(fullQualifiedClassname, containingFile.getModule(), method);
               entities.add(entitity);
            }
         } else {
            ChangedEntity entitity = new ChangedEntity(fullQualifiedClassname, containingFile.getModule());
            entities.add(entitity);
         }
      }
      return entities;
   }

   @JsonIgnore
   public Set<ChangedEntity> getChanges() {
      Set<ChangedEntity> entities = new HashSet<>();
      for (Map.Entry<String, Set<String>> change : changedMethods.entrySet()) {
         String fullQualifiedClassname = getFQN(change.getKey());
         if (change.getValue() != null) {
            for (String method : change.getValue()) {
               if (method.contains("(")) {
                  String methodWithoutParameters = method.substring(0, method.indexOf('('));
                  ChangedEntity entity = new ChangedEntity(fullQualifiedClassname, containingFile.getModule(), methodWithoutParameters);
                  entity.createParameters(method.substring(method.indexOf('(')));
                  entities.add(entity);
               } else {
                  ChangedEntity entity = new ChangedEntity(fullQualifiedClassname, containingFile.getModule(), method);
                  entities.add(entity);
               }

            }
         } else {
            ChangedEntity entity = new ChangedEntity(fullQualifiedClassname, containingFile.getModule());
            entities.add(entity);
         }
      }
      return entities;
   }

   private String getFQN(final String className) {
      String fullQualifiedClassname;
      LOG.trace("Containing file: {} {} package: {}", containingFile, className, containingFile.getPackage());
      if (!"".equals(containingFile.getPackage())) {
         fullQualifiedClassname = containingFile.getPackage() + "." + className;
      } else {
         fullQualifiedClassname = className;
      }
      return fullQualifiedClassname;
   }

   public Set<ChangedEntity> getImportChanges() {
      return importChanges;
   }

   public void addImportChange(final String name, final List<ChangedEntity> entities) {
      importChanges.add(new ChangedEntity(name, ""));
      isChange = true;
      isOnlyMethodChange = false;

      for (ChangedEntity entity : entities) {
         addClazzChange(entity);
      }
   }

}