package de.peran;

/*-
 * #%L
 * peran-dependency
 * %%
 * Copyright (C) 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.peran.dependency.analysis.data.TraceElement;
import de.peran.dependency.traces.TraceReadUtils;

public class TestFinding {
	
	@Test
	public void testAnonymousClass() throws FileNotFoundException{
		final CompilationUnit cu =  JavaParser.parse(new File("src/test/resources/methodFinding/AnonymousClassExample.java"));
		
		final TraceElement anonymousTrace = new TraceElement("AnonymousClassExample$1", "run", 1);
		final Node anonymousMethod = TraceReadUtils.getMethod(anonymousTrace, cu);
		
		Assert.assertNotNull(anonymousMethod);
		
		final TraceElement elementConstuctor = new TraceElement("AnonymousClassExample$MyPrivateClass", "<init>", 1);
		final Node methodConstructor = TraceReadUtils.getMethod(elementConstuctor, cu);
		
		Assert.assertNotNull(methodConstructor);
		
		final TraceElement elementInnerMethod = new TraceElement("AnonymousClassExample$MyPrivateClass", "doSomething", 1);
		final Node innerMethod = TraceReadUtils.getMethod(elementInnerMethod, cu);
		
		Assert.assertNotNull(innerMethod);
	}
}
