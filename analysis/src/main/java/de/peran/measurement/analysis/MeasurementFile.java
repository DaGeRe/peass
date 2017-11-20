package de.peran.measurement.analysis;

import java.io.File;

/**
 * Represents one result file with measurements
 * @author reichelt
 *
 */
class MeasurementFile {
	private File file;
	private String version;
	private String method, clazz;

	MeasurementFile(final File file, final String version, final String clazz, final String method) {
		super();
		this.file = file;
		this.version = version;
		this.method = method;
		this.clazz = clazz;   
	}

	public File getFile() {
		return file;
	}

	public void setFile(final File file) {
		this.file = file;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(final String clazz) {
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return "MeasurementFile [file=" + file + ", version=" + version + ", method=" + method + ", clazz=" + clazz + "]";
	}
}