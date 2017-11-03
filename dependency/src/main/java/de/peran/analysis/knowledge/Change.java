package de.peran.analysis.knowledge;

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


/**
 * Saves information about one change
 * 
 * @author reichelt
 *
 */
public class Change {
	private String diff;
	private String clazz;
	private double changePercent;
	private String correctness;
	private String type;

	public String getCorrectness() {
		return correctness;
	}

	public void setCorrectness(final String correctness) {
		this.correctness = correctness;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getDiff() {
		return diff;
	}

	public void setDiff(final String diff) {
		this.diff = diff;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(final String clazz) {
		this.clazz = clazz;
	}

	public double getChangePercent() {
		return changePercent;
	}

	public void setChangePercent(final double changePercent) {
		this.changePercent = changePercent;
	}
}
