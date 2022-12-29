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

package de;

import junit.framework.TestCase;


public class TestMe1 extends TestCase{
	
	public void testMe1(){
		System.out.println("This is a test.");
	}

	public void setUp(){
		System.out.println("Just a setup.");
	}

	public void tearDown(){
		System.out.println("Just a teardown.");
	}	
}

class LocalClass {
  private int i;
  
  void myMethod(int i){
     this.i = i;
  }
}

enum LocalEnum {

}

interface LocalInterface { 

}
