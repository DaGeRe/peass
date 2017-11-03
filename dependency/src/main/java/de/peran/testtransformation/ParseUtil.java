package de.peran.testtransformation;

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


import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

/**
 * Helper class for parsing Java code with Javaparser.
 * 
 * @author reichelt
 *
 */
public final class ParseUtil {

	/**
	 * Prive Constructor - Do not Initialize Helper class.
	 */
	private ParseUtil() {

	}

	/**
	 * Returns the first ClassOrInterfaceDeclaration of a CompilationUnit ATTENTION: If multiple classes are declared, the first is returned (may be the case if one class/interface is non-public).
	 * 
	 * @param unit CompilationUnit, which is searched for a class declaration
	 * @return Declration if found, else null
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
