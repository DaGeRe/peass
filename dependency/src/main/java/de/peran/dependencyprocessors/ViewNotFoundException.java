package de.peran.dependencyprocessors;

/**
 * Occurs when a view, i.e. trace-diff of testcalls in two versions, can not be generated because kieker-results are not available.
 * @author reichelt
 *
 */
public class ViewNotFoundException extends Exception{

	public ViewNotFoundException(String string) {
		super(string);
	}

}
