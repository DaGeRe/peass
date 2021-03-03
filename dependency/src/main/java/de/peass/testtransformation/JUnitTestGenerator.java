package de.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.ClazzFinder;

public class JUnitTestGenerator {

   private final File module;
   private final ChangedEntity generatedName;
   private final ChangedEntity callee;
   private final String method;

   private final JUnitTestTransformer transformer;

   public JUnitTestGenerator(final File module, final ChangedEntity generatedName, final ChangedEntity callee, final String method, final JUnitTestTransformer transformer) {
      this.module = module;
      this.generatedName = generatedName;
      this.callee = callee;
      this.method = method;
      this.transformer = transformer;
   }

   public File generateClazz() {
      final File calleeClazzFile = ClazzFileFinder.getClazzFile(module, callee);
      final int version = transformer.getVersion(calleeClazzFile);

      String testFolderName = calleeClazzFile.getAbsolutePath().contains("src " + File.separator + "test " + File.separator + "java") ? "src" + File.separator +"test" + File.separator + "java" : "src" + File.separator + "test";
      final File testClazzFolder = new File(module, testFolderName);
      final String packageFolder = generatedName.getPackage().replaceAll("\\.", File.separator);
      final File generatedClassFile = new File(testClazzFolder, packageFolder + File.separator + generatedName.getSimpleClazzName() + ".java");
      generatedClassFile.getParentFile().mkdirs();
      final CompilationUnit generatedCu = new CompilationUnit();
      generatedCu.setPackageDeclaration(generatedName.getPackage());

      final CompilationUnit calleeUnit = transformer.getLoadedFiles().get(calleeClazzFile);
      generatedCu.getImports().addAll(calleeUnit.getImports());
      generatedCu.getImports().add(new ImportDeclaration(callee.getJavaClazzName(), false, false));

      final ClassOrInterfaceDeclaration generatedClass = generatedCu.addClass(generatedName.getSimpleClazzName());
      if (version == 3) {
         generatedClass.getExtendedTypes().add(new ClassOrInterfaceType("TestCase"));
      }
      generatedClass.addField(callee.getSimpleClazzName(), "sut", Modifier.privateModifier().getKeyword());

      final ClassOrInterfaceDeclaration calleeClass = ClazzFinder.findClazz(callee, calleeUnit.getChildNodes());

      List<ClassOrInterfaceDeclaration> superClazzes = getSuperclasses(calleeClass, calleeUnit);

      addMethods(calleeClass, callee, generatedClass, superClazzes);
      addFields(calleeClass, generatedClass);

      generateTestMethod(calleeClazzFile, generatedClass);

      if (version == 4) {
         transformer.editJUnit4(generatedCu);
      } else {
         transformer.editJUnit3(generatedCu);
      }

      try {
         FileUtils.writeStringToFile(generatedClassFile, generatedCu.toString(), Charset.defaultCharset());
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return generatedClassFile;
   }

   List<ClassOrInterfaceDeclaration> getSuperclasses(final ClassOrInterfaceDeclaration calleeClass, final CompilationUnit unit) {
      List<ClassOrInterfaceDeclaration> superClazzes = new LinkedList<>();
      for (ClassOrInterfaceType superclass : calleeClass.getExtendedTypes()) {
         if (!superclass.getNameAsString().equals("TestCase") && !superclass.getNameAsString().equals("junit.framework.TestCase")
               && !superclass.getNameAsString().equals("KoPeMeTestcase")) {
            String superPackage = null;
            for (ImportDeclaration importDecl : unit.getImports()) {
               String declaration = importDecl.getNameAsString();
               String clazzName = declaration.substring(declaration.lastIndexOf('.') + 1);
               if (clazzName.equals(superclass.getNameAsString())) {
                  superPackage = declaration;
               }
            } // TODO Other module, same package, asterix-import
            ChangedEntity superEntity = new ChangedEntity(superPackage != null ? superPackage : superclass.getNameAsString(), callee.getModule());
            final File superClazzFile = ClazzFileFinder.getClazzFile(module, superEntity);
            CompilationUnit superUnit = transformer.getLoadedFiles().get(superClazzFile);
            final ClassOrInterfaceDeclaration superClass = ClazzFinder.findClazz(superEntity, superUnit.getChildNodes());
            makeSetUpPublic(superClazzFile, superUnit, superClass);

            superClazzes.add(superClass);
            if (superClass.getExtendedTypes().size() != 0) {
               superClazzes.addAll(getSuperclasses(superClass, superUnit));
            }
         }
      }
      return superClazzes;
   }

   void makeSetUpPublic(final File superClazzFile, final CompilationUnit superUnit, final ClassOrInterfaceDeclaration superClass) {
      boolean change = false;
      for (MethodDeclaration method : superClass.getMethods()) {
         if (!method.getModifiers().contains(Modifier.publicModifier())) {
            if (method.getNameAsString().equals("setUp") || method.getNameAsString().equals("tearDown")
                  || method.getAnnotationByName("BeforeClass").isPresent()
                  || method.getAnnotationByName("AfterClass").isPresent()
                  || method.getAnnotationByName("Before").isPresent()
                  || method.getAnnotationByName("After").isPresent()) {
               method.getModifiers().remove(Modifier.protectedModifier());
               method.getModifiers().add(Modifier.publicModifier());
               change = true;
            }
         }
      }
      if (change) {
         try {
            FileUtils.writeStringToFile(superClazzFile, superUnit.toString(), Charset.defaultCharset());
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   private MethodDeclaration generateTestMethod(final File clazzFile, final ClassOrInterfaceDeclaration generatedClass) {
      final NodeList<Modifier> modifiers = new NodeList<>(Modifier.publicModifier());
      final MethodDeclaration methodDeclaration = new MethodDeclaration(modifiers, new VoidType(), method);
      methodDeclaration.setModifiers(modifiers);
      methodDeclaration.getThrownExceptions().add(new ClassOrInterfaceType("java.lang.Throwable"));
      generatedClass.addMember(methodDeclaration);

      final BlockStmt block = new BlockStmt();
      block.addStatement("sut." + method + "();");
      methodDeclaration.setBody(block);

      final MethodDeclaration methodOld = findMethod(clazzFile, method);
      methodOld.setThrownExceptions(methodOld.getThrownExceptions());
      for (final AnnotationExpr annotation : methodOld.getAnnotations()) {
         methodDeclaration.addAnnotation(annotation);
      }

      return methodDeclaration;
   }

   public MethodDeclaration findMethod(final File clazzFile, final String methodName) {
      MethodDeclaration methodOld = null;
      final CompilationUnit unit = transformer.getLoadedFiles().get(clazzFile);
      final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);

      for (final MethodDeclaration method : clazz.getMethods()) {
         if (method.getNameAsString().equals(methodName)) {
            methodOld = method;
         }
      }

      return methodOld;
   }

   private void addFields(final ClassOrInterfaceDeclaration calleeClass, final ClassOrInterfaceDeclaration generatedClass) {
      for (final FieldDeclaration field : calleeClass.getFields()) {
         final Modifier.Keyword[] keywords = getKeyword(field.getModifiers());
         String name = "unknown";
         Expression initializer = null;
         String type = null;
         for (final Node child : field.getChildNodes()) {
            if (child instanceof VariableDeclarator) {
               final VariableDeclarator declarator = (VariableDeclarator) child;
               name = declarator.getNameAsString();
               if (declarator.getInitializer().isPresent()) {
                  initializer = declarator.getInitializer().get();
               }
               type = declarator.getTypeAsString();
            }
         }
         final FieldDeclaration newField;
         if (initializer != null) {
            newField = generatedClass.addFieldWithInitializer(type, name, initializer, keywords);
         } else {
            newField = generatedClass.addField(type, name, keywords);
         }
         for (final AnnotationExpr annotation : field.getAnnotations()) {
            newField.addAnnotation(annotation);
         }
      }
   }

   private void addMethods(final ClassOrInterfaceDeclaration calleeClass, final ChangedEntity callee, final ClassOrInterfaceDeclaration generatedClass,
         final List<ClassOrInterfaceDeclaration> superclasses) {

      final List<ConstructorDeclaration> calleeConstructors = calleeClass.getConstructors();
      if (calleeConstructors.size() > 0) {
         for (final ConstructorDeclaration constructor : calleeConstructors) {
            final ConstructorDeclaration newConstructor = generatedClass.addConstructor(Modifier.publicModifier().getKeyword());
            newConstructor.setParameters(constructor.getParameters());
            newConstructor.setThrownExceptions(constructor.getThrownExceptions());

            final String parameters = getParameters(constructor.getParameters());

            final BlockStmt block = new BlockStmt();
            block.addStatement("super(" + parameters + ");");
            block.addStatement("sut = new " + callee.getJavaClazzName() + "(" + parameters + ");");
            newConstructor.setBody(block);
         }
      } else {
         final ConstructorDeclaration newConstructor = generatedClass.addConstructor(Modifier.publicModifier().getKeyword());
         final BlockStmt block = new BlockStmt();
         block.addStatement("super();");
         block.addStatement("sut = new " + callee.getJavaClazzName() + "();");
         newConstructor.setBody(block);
      }

      for (String annotationName : new String[] { "BeforeClass", "AfterClass", "Before", "After" }) {
         boolean found = findMethodByAnnotation(calleeClass, generatedClass, annotationName);
         for (ClassOrInterfaceDeclaration superclass : superclasses) {
            found = findMethodByAnnotation(superclass, generatedClass, annotationName);
            if (found) {
               break;
            }
         }
      }

      for (String methodName : new String[] { "setUp", "tearDown" }) {
         boolean found = findMethodByName(calleeClass, generatedClass, methodName);
         if (!found) {
            for (ClassOrInterfaceDeclaration superclass : superclasses) {
               found = findMethodByName(superclass, generatedClass, methodName);
               if (found) {
                  break;
               }
            }
         }
      }

      // for (final MethodDeclaration method : calleeClass.getMethods()) {
      // if (method.getNameAsString().equals("setUp") || method.getNameAsString().equals("tearDown") ||
      // method.getAnnotationByName("BeforeClass").isPresent() || method.getAnnotationByName("AfterClass").isPresent() ||
      // method.getAnnotationByName("Before").isPresent() || method.getAnnotationByName("After").isPresent()) {
      // copyMethod(generatedClass, method);
      // }
      // }
   }

   boolean findMethodByName(final ClassOrInterfaceDeclaration calleeClass, final ClassOrInterfaceDeclaration generatedClass, final String methodName) {
      boolean found = false;
      for (final MethodDeclaration method : calleeClass.getMethods()) {
         if (method.getNameAsString().equals(methodName)) {
            copyMethod(generatedClass, method);
            found = true;
         }
      }
      return found;
   }

   boolean findMethodByAnnotation(final ClassOrInterfaceDeclaration calleeClass, final ClassOrInterfaceDeclaration generatedClass, final String annotationName) {
      boolean found = false;
      for (final MethodDeclaration method : calleeClass.getMethods()) {
         if (method.getAnnotationByName(annotationName).isPresent()) {
            copyMethod(generatedClass, method);
            found = true;
         }
      }
      return found;
   }

   private void copyMethod(final ClassOrInterfaceDeclaration generatedClass, final MethodDeclaration method) {
      for (MethodDeclaration alreadyCreatedMethod : generatedClass.getMethods()) {
         if (alreadyCreatedMethod.getNameAsString().equals(method.getNameAsString())) {
            System.out.println("Do not copy!");
            return;
         }
      }

      final Modifier.Keyword[] keywords = getKeyword(method.getModifiers());
      final MethodDeclaration newMethod = generatedClass.addMethod(method.getNameAsString(), keywords);
      newMethod.setThrownExceptions(method.getThrownExceptions());
      final String parameters = getParameters(method.getParameters());

      final BlockStmt block = new BlockStmt();
      block.addStatement("sut." + method.getNameAsString() + "(" + parameters + ");");
      newMethod.setBody(block);

      for (final AnnotationExpr annotation : method.getAnnotations()) {
         newMethod.addAnnotation(annotation);
      }
   }

   private Modifier.Keyword[] getKeyword(final NodeList<Modifier> modifiers) {
      final Modifier.Keyword[] keywords = new Modifier.Keyword[modifiers.size()];
      for (int i = 0; i < modifiers.size(); i++) {
         keywords[i] = modifiers.get(i).getKeyword();
      }
      return keywords;
   }

   String getParameters(final List<Parameter> parametersOld) {
      String parameters = "";
      for (final Parameter param : parametersOld) {
         parameters += param.getNameAsString() + ",";
      }
      if (parameters.length() > 0) {
         parameters = parameters.substring(0, parameters.length() - 1);
      }
      return parameters;
   }
}
