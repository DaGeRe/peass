package de.peran.dependency.traces.requitur.content;

import java.util.Arrays;

import de.peran.dependency.analysis.data.TraceElement;

public class TraceElementContent extends Content {
	TraceElement value;

	private final String clazz, method;

	private final boolean isStatic;

	private final String[] parameterTypes;

	private final int depth;

//	private String source;

	public TraceElementContent(final TraceElement element) {
		this.clazz = element.getClazz();
		this.method = element.getMethod();
		this.parameterTypes = element.getParameterTypes();
		this.depth = element.getDepth();
		this.isStatic = element.isStatic();
	}

	public TraceElementContent(final String clazz, final String method, final int depth) {
		this.clazz = clazz;
		this.method = method;
		this.depth = depth;
		this.parameterTypes = new String[0];
		this.isStatic = false;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof TraceElementContent) {
			final TraceElementContent other = (TraceElementContent) obj;
			if (((TraceElementContent) obj).getMethod().equals("main")){
				System.out.println(parameterTypes);
			}
			return other.getClazz().equals(this.getClazz()) &&
					other.getMethod().equals(this.getMethod()) &&
					Arrays.equals(other.getParameterTypes(), this.getParameterTypes());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return clazz.hashCode() + method.hashCode();
	}

	@Override
	public String toString() {
		if (parameterTypes.length == 0) {
			return clazz + "#" + method;
		} else {
			return clazz + "#" + method + "(" + Arrays.deepToString(parameterTypes) + ")";
		}
	}

	public String getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}

	public String getSimpleClazz() {
		final String simpleClazz = clazz.substring(clazz.lastIndexOf('.') + 1);
		if (simpleClazz.contains("$")) {
			return simpleClazz.substring(simpleClazz.lastIndexOf("$") + 1);
		}
		return simpleClazz;
	}

	public int getDepth() {
		return depth;
	}

	public String[] getParameterTypes() {
		return parameterTypes;
	}

//	public String getSource() {
//		return source;
//	}
//
//	public void setSource(final String source) {
//		this.source = source;
//	}
}