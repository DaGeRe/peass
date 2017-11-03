package de.peran.measurement.analysis;

/*-
 * #%L
 * peran-analysis
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
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
