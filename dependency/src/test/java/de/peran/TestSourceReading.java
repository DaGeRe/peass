package de.peran;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.peran.dependency.traces.TraceReadUtils;
import de.peran.dependency.traces.requitur.content.TraceElementContent;

public class TestSourceReading {
	
	@Test
	public void testAnonymousClass() throws FileNotFoundException{
		final CompilationUnit cu =  JavaParser.parse(new File("src/test/resources/methodFinding/AnonymousClassExample.java"));
		
		final TraceElementContent anonymousTrace = new TraceElementContent("AnonymousClassExample$1", "run", 1);
		final Node anonymousMethod = TraceReadUtils.getMethod(anonymousTrace, cu);
		
		Assert.assertNotNull(anonymousMethod);
		
		final TraceElementContent elementConstuctor = new TraceElementContent("AnonymousClassExample$MyPrivateClass", "<init>", 1);
		final Node methodConstructor = TraceReadUtils.getMethod(elementConstuctor, cu);
		
		Assert.assertNotNull(methodConstructor);
		
		final TraceElementContent elementInnerMethod = new TraceElementContent("AnonymousClassExample$MyPrivateClass", "doSomething", 1);
		final Node innerMethod = TraceReadUtils.getMethod(elementInnerMethod, cu);
		
		Assert.assertNotNull(innerMethod);
	}
}
