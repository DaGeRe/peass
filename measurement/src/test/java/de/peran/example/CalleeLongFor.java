package de.peran.example;

/*-
 * #%L
 * peran-measurement
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


public class CalleeLongFor {

	private int j = 1;
	
	public void callMe() {
		for (int i = 0; i < 10; i++) {
			callMe2();
			callMe3();
		}
	}

	private void callMe2() {

	}

	private void callMe3() {
		j = callMe4()+2;
	}
	
	private int callMe4() {
		return j+1;
	}

}
