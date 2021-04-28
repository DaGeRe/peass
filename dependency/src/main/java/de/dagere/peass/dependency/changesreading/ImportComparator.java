package de.dagere.peass.dependency.changesreading;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.ImportDeclaration;

public class ImportComparator {
   
   private final Set<ImportDeclaration> notInBothImport = new HashSet<>();

   public ImportComparator(List<ImportDeclaration> imports1, List<ImportDeclaration> imports2) {
      final Set<String> importSet1 = getImportNames(imports1);
      final Set<String> importSet2 = getImportNames(imports2);
      
      Set<String> notIn2 = new HashSet<>(importSet1);
      notIn2.removeAll(importSet2);
      
      Set<String> notIn1 = new HashSet<>(importSet2);
      notIn1.removeAll(importSet1);
      
      Set<String> notInBoth = notIn1;
      notInBoth.addAll(notIn2);
      
      for (String importName : notInBoth) {
         notInBothImport.add(new ImportDeclaration(importName, false, false));
      }
   }

   public Set<String> getImportNames(List<ImportDeclaration> imports1) {
      final Set<String> importSet1 = new HashSet<>();
      for (ImportDeclaration imports : imports1) {
         importSet1.add(imports.getNameAsString());
      }
      return importSet1;
   }
   
   public Set<ImportDeclaration> getNotInBoth() {
      return notInBothImport;
   }
}
