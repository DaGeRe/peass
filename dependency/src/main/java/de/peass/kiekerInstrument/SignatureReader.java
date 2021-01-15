package de.peass.kiekerInstrument;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;

public class SignatureReader {
   
   private final CompilationUnit unit;
   private final String name;
   
   public SignatureReader(CompilationUnit unit, String name) {
      this.unit = unit;
      this.name = name;
   }

   public String getSignature(MethodDeclaration method) {
      String modifiers = getModifierString(method.getModifiers());
      String returnTypFQN = getTypeFQN(method.getType());
      final String returnType = returnTypFQN + " ";
      String signature = modifiers + returnType + name + "(";
      signature += getParameterString(method);
      signature += ")";
      return signature;
   }
   
   public String getSignature(ConstructorDeclaration method) {
      String modifiers = getModifierString(method.getModifiers());
      String signature = modifiers + "new " + name + ".<init>(" + ")";
      return signature;
   }

   private String getParameterString(MethodDeclaration method) {
      String parameterString = "";
      for (Parameter parameter : method.getParameters()) {
         String fqn = getTypeFQN(parameter.getType());
         parameterString += fqn + ",";
      }
      if (parameterString.length() > 0) {
         parameterString = parameterString.substring(0, parameterString.length() - 1);
      }
      return parameterString;
   }

   private String getTypeFQN(Type type) {
      String typeName = type.asString();
      if (typeName.equals("void")) {
         return typeName;
      }
      String fqn;
      if (type.isPrimitiveType()) {
         fqn = typeName;
      } else if (type.isArrayType()) {
         ArrayType arrayType = (ArrayType) type;
         Type componentType = arrayType.getComponentType();
         String arrayString = arrayType.asString().substring(arrayType.asString().indexOf('['));
         if (componentType.isPrimitiveType()) {
            fqn = typeName ;
         } else {
            String typeNameWithoutArray = typeName.substring(0, typeName.indexOf('['));
            ImportDeclaration currentImport = findImport(typeNameWithoutArray);
            if (currentImport != null) {
               fqn = currentImport.getNameAsString() + arrayString;
            } else {
               fqn = "java.lang." + typeName;
            }
         }
      } else {
         ImportDeclaration currentImport = findImport(typeName);
         if (currentImport != null) {
            fqn = currentImport.getNameAsString();
         } else {
            fqn = "java.lang." + typeName;
         }
      }
      return fqn;
   }

   private ImportDeclaration findImport(String typeName) {
      ImportDeclaration currentImport = null;
      for (ImportDeclaration importDeclaration : unit.getImports()) {
         final String importFqn = importDeclaration.getNameAsString();
         if (importFqn.endsWith("." + typeName)) {
            currentImport = importDeclaration;
            break;
         }
      }
      return currentImport;
   }

   private String getModifierString(NodeList<Modifier> listOfModifiers) {
      String modifiers = "";
      for (Modifier modifier : listOfModifiers) {
         modifiers += modifier;
      }
      return modifiers;
   }
}
