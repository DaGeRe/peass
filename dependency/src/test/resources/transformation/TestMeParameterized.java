/**
 *     This file is part of Peass.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.Before;
import org.junit.After;

public class TestMeParameterized {

	@DisplayName("parameterTest")
   @ParameterizedTest(name = "run #{index} with [parameter1: {0}, parameter2: {1}]")
   @CsvSource({ "1, 0", "2, 0", "3, 3" })
	public void testMethod1(){
		System.out.println("This is tested.");
	}
}
