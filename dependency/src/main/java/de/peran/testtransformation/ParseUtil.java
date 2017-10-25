package de.peran.testtransformation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class ParseUtil {

	/**
	 * Returns the first ClassOrInterfaceDeclaration of a CompilationUnit ATTENTION: If multiple classes are declared, the first is returned (may be the case if one class/interface is non-public)
	 * 
	 * @param unit
	 * @return
	 */
	public static ClassOrInterfaceDeclaration getClass(final CompilationUnit unit) {
		for (final Node node : unit.getChildNodes()) {
			if (node instanceof ClassOrInterfaceDeclaration) {
				return (ClassOrInterfaceDeclaration) node;
			}
		}
		return null;
	}
}
