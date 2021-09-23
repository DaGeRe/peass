package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.PrimitiveType;

import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import net.kieker.sourceinstrumentation.instrument.TypeInstrumenter;

public class TestTypeInstrumentation {

   @Test
   public void testEnumInstrumentation() throws IOException {
      CompilationUnit unit = JavaParserProvider.parse(new File("src/test/resources/sourceInstrumentation/project_2_interface/src/main/java/de/peass/SomeEnum.java"));
      EnumDeclaration declaration = unit.findAll(EnumDeclaration.class).get(0);
      
      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, false);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, unit);
      instrumenter.handleTypeDeclaration(declaration, "de.dagere.test");
      
      ConstructorDeclaration defaultConstructor = declaration.findAll(ConstructorDeclaration.class).get(0);
      
      System.out.println(defaultConstructor);
      MatcherAssert.assertThat(defaultConstructor.toString(), Matchers.not(Matchers.containsString("_kieker_sourceInstrumentation_controller.isProbeActivated")));
   }
   
   @Test
   public void testBasicInstrumentation() throws IOException {
      ClassOrInterfaceDeclaration clazz = buildClass();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, false);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class));
      boolean hasChange = instrumenter.handleTypeDeclaration(clazz, "de.dagere.test");

      Assert.assertTrue(hasChange);

      MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("long _kieker_sourceInstrumentation_tout = _kieker_sourceInstrumentation_TIME_SOURCE.getTime();"));
   }

   @Test
   public void testExtractionInstrumentation() throws IOException {
      ClassOrInterfaceDeclaration clazz = buildClass();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, true);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class));
      boolean hasChange = instrumenter.handleTypeDeclaration(clazz, "de.dagere.test");

      Assert.assertTrue(hasChange);

      List<MethodDeclaration> myMethod = clazz.getMethodsByName(InstrumentationConstants.PREFIX + "myMethod");
      Assert.assertEquals(1, myMethod.size());
      MatcherAssert.assertThat(myMethod.get(0).getModifiers(), Matchers.containsInAnyOrder(Modifier.privateModifier(), Modifier.finalModifier()));
      MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("private final int " + InstrumentationConstants.PREFIX + "myMethod()"));

      List<MethodDeclaration> myRegularMethod = clazz.getMethodsByName(InstrumentationConstants.PREFIX + "myRegularMethod");
      Assert.assertEquals(1, myRegularMethod.size());
      Assert.assertEquals(myRegularMethod.get(0).getDeclarationAsString(), "private final void " + InstrumentationConstants.PREFIX + "myRegularMethod(int someParameter)");
      
      List<MethodDeclaration> myRegularMethodInstrumented = clazz.getMethodsByName("myRegularMethod");
      MatcherAssert.assertThat(myRegularMethodInstrumented.get(0).toString(), Matchers.containsString(InstrumentationConstants.PREFIX + "myRegularMethod(someParameter)"));
      
      MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("private final static void " + InstrumentationConstants.PREFIX + "myStaticMethod()"));
      System.out.println(clazz.toString());
   }

   private ClassOrInterfaceDeclaration buildClass() {

      ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration();
      clazz.setName("MyClazz");

      addSimpleMethod(clazz);
      addMethodWithParameters(clazz);
      addStaticMethod(clazz);

      return clazz;
   }

   private void addSimpleMethod(final ClassOrInterfaceDeclaration clazz) {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      simpleBlock.addStatement(new ReturnStmt("return 5"));
      MethodDeclaration method = clazz.addMethod("myMethod");
      method.setBody(simpleBlock);
      method.setType("int");
   }

   private void addMethodWithParameters(final ClassOrInterfaceDeclaration clazz) {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      MethodDeclaration method = clazz.addMethod("myRegularMethod", Modifier.Keyword.PRIVATE);
      method.getParameters().add(new Parameter(PrimitiveType.intType(), "someParameter"));
      method.setBody(simpleBlock);
   }

   private void addStaticMethod(final ClassOrInterfaceDeclaration clazz) {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      MethodDeclaration method = clazz.addMethod("myStaticMethod", Modifier.Keyword.STATIC);
      method.setBody(simpleBlock);
   }

}
