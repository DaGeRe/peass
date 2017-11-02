package de.peran;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.peran.dependency.analysis.data.TraceElement;
import de.peran.dependency.traces.TraceReadUtils;

public class TestSourceDetection {
	private static final File SOURCE = new File("src/test/resources/detection/");
	
	@Test
	public void testAnonymousClazzes() throws ParseException, IOException{
		final File file = new File(SOURCE, "Test1_Anonym.java");
		CompilationUnit cu = JavaParser.parse(file);
		
		TraceElement currentTraceElement = new TraceElement("Test1_Anonym$1", "<init>", 1);
		currentTraceElement.setParameterTypes(new String[0]);
		Node method = TraceReadUtils.getMethod(currentTraceElement , cu);
		
		System.out.println(method);
		
		Assert.assertNull(method);
		
		TraceElement traceElementRun1 = new TraceElement("Test1_Anonym$1", "run", 1);
		traceElementRun1.setParameterTypes(new String[0]);
		Node methodRun = TraceReadUtils.getMethod(traceElementRun1 , cu);
		
		System.out.println(methodRun);
		
		Assert.assertNotNull(methodRun);
		Assert.assertThat(methodRun.toString(), Matchers.containsString("Run R3"));
		
		TraceElement traceElementRun2 = new TraceElement("Test1_Anonym$2", "run", 1);
		traceElementRun2.setParameterTypes(new String[0]);
		Node methodRun2 = TraceReadUtils.getMethod(traceElementRun2 , cu);
		
		System.out.println(methodRun2);
		
		Assert.assertNotNull(methodRun2);
		Assert.assertThat(methodRun2.toString(), Matchers.containsString("Run R1"));
		
		TraceElement traceElementRun3 = new TraceElement("Test1_Anonym$3", "run", 1);
		traceElementRun3.setParameterTypes(new String[0]);
		Node methodRun3 = TraceReadUtils.getMethod(traceElementRun3 , cu);
		
		System.out.println(methodRun3);
		
		Assert.assertNotNull(methodRun3);
		Assert.assertThat(methodRun3.toString(), Matchers.containsString("Run R2"));
	}
	
	@Test
	public void testNamedClazzes() throws ParseException, IOException{
		final File file = new File(SOURCE, "Test2_Named.java");
		CompilationUnit cu = JavaParser.parse(file);
		
		TraceElement currentTraceElement = new TraceElement("Test2_Named$MyStuff", "doMyStuff1", 1);
		currentTraceElement.setParameterTypes(new String[0]);
		Node methodRun = TraceReadUtils.getMethod(currentTraceElement , cu);
		
		System.out.println(methodRun);
		
		Assert.assertNotNull(methodRun);
		Assert.assertThat(methodRun.toString(), Matchers.containsString("stuff 1"));
		
		TraceElement currentTraceElement2 = new TraceElement("Test2_Named$MyStuff2", "doMyStuff2", 1);
		currentTraceElement2.setParameterTypes(new String[0]);
		Node methodRun2 = TraceReadUtils.getMethod(currentTraceElement2 , cu);
		
		System.out.println(methodRun2);
		
		Assert.assertNotNull(methodRun2);
		Assert.assertThat(methodRun2.toString(), Matchers.containsString("stuff 2"));
	}
	
	@Test
	public void testAnonymousList() throws FileNotFoundException{
		final File file = new File(SOURCE, "Test1_Anonym.java");
		CompilationUnit cu = JavaParser.parse(file);
		List<NodeList<BodyDeclaration<?>>> anonymous = TraceReadUtils.getAnonymusClasses(cu);
		
		Assert.assertEquals(3, anonymous.size());
		
		Assert.assertThat(anonymous.get(0).get(0).toString(), Matchers.containsString("Run R3"));
		Assert.assertThat(anonymous.get(1).get(0).toString(), Matchers.containsString("Run R1"));
		Assert.assertThat(anonymous.get(2).get(0).toString(), Matchers.containsString("Run R2"));
	}
	
	@Test
	public void testNamedList() throws FileNotFoundException{
		final File file = new File(SOURCE, "Test2_Named.java");
		CompilationUnit cu = JavaParser.parse(file);
		Map<String, ClassOrInterfaceDeclaration> named = TraceReadUtils.getNamedClasses(cu);
		
		Assert.assertEquals(3, named.size());
		
		Assert.assertThat(named.get("Test2_Named$MyStuff").toString(), Matchers.containsString("stuff 1"));
		Assert.assertThat(named.get("Test2_Named$MyStuff2").toString(), Matchers.containsString("stuff 2"));
	}
	
	@Test
	public void testDirectoryWalker() throws FileNotFoundException{
		final File file = new File(SOURCE, "DirectoryWalkerTestCase.java");
		CompilationUnit cu = JavaParser.parse(file);
		Map<String, ClassOrInterfaceDeclaration> named = TraceReadUtils.getNamedClasses(cu);
		
		Assert.assertEquals(4, named.size());
		
		Assert.assertThat(named.get("DirectoryWalkerTestCase$TestFileFinder").toString(), Matchers.containsString("List results"));
	}
}
