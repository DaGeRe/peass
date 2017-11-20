/**
 *     This file is part of PerAn.
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
import java.io.File;
import java.lang.*;
import some.stuff.that.does.not.exist;

/**
 * Example comment
 * @author reichelt
 *
 */
class Test{
	
	public static final int y = 438;
	public int w = 48;

	/**
	 * Comment 1
	 */
	public Test() {
		// Line-comment
		int a = 3 + 5 - 8;
		System.out.println(a);
	}
	
	/**
	 * Comment 2
	 * @param i
	 */
	public static void doStaticThing(int i){
		int y = i + 1;
		System.out.println(y);
	}
	
	/**
	 * Comment 3
	 */
	public void doNonStaticThing(){
		System.out.println("a");
	}
}