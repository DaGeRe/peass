package de.peran.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

/**
 * Searches for all classes in a maven project. Used for instrumeting them.
 * 
 * @author reichelt
 *
 */
public class ClazzFinder {

	/**
	 * Returns a list of all classes of a maven project
	 * 
	 * @param projectFolder Folder where to search for classes
	 * @return list of classes
	 */
	public static List<String> getClasses(final File projectFolder) {
		List<String> clazzes = new LinkedList<>();
		File src = new File(projectFolder, "src");
		File main = new File(src, "main");
		File mainJava = new File(src, "java");
		File test = new File(src, "test");
		if (mainJava.exists()) {
			addClazzes(clazzes, mainJava);
		} else {
			if (main.exists()) {
				File java = new File(main, "java");
				if (java.exists()) {
					addClazzes(clazzes, java);
				} else {
					addClazzes(clazzes, main);
				}
			}
		}

		if (test.exists()) {
			File java = new File(test, "java");
			if (java.exists()) {
				addClazzes(clazzes, java);
			} else {
				addClazzes(clazzes, test);
			}
		}
		return clazzes;
	}

	/**
	 * Searches for classes in a specific folder
	 * 
	 * @param clazzes List where classes should be added
	 * @param folder Main folder that should be searched
	 */
	private static void addClazzes(final List<String> clazzes, final File folder) {
		for (File clazzFile : FileUtils.listFiles(folder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
			String path = clazzFile.getAbsolutePath();
			path = path.replace(folder.getAbsolutePath() + File.separator, "");
			path = path.substring(0, path.length() - 5);
			final String clazz = path.replace(File.separator, ".");
			clazzes.add(clazz);
			
			try {
				final CompilationUnit cu = JavaParser.parse(clazzFile);
				for (Node node : cu.getChildNodes()){
					clazzes.addAll(getClazzes(node, clazz));
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	private static List<String> getClazzes(final Node node, final String parent){
		List<String> clazzes = new LinkedList<>();
		if (node instanceof ClassOrInterfaceDeclaration){
			ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) node;
			if (clazz.getParentNode().isPresent()){
				String clazzname = parent + "." + clazz.getName().getIdentifier();
				clazzes.add(clazzname);
			}
		}
		for (Node child : node.getChildNodes()){
			clazzes.addAll(getClazzes(child, parent));
		}
		return clazzes;
	}

}
