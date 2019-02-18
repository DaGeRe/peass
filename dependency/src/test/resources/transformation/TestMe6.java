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
import junit.framework.TestCase;
import de.dagere.kopeme.junit3.KoPeMeTestcase;
import de.dagere.kopeme.datacollection.DataCollectorList;

public class TestMe6 extends KoPeMeTestcase{
	
    public void testMe1(){
        System.out.println("This is a test.");
    }

    public int getWarmupExecutions() {
        return 10;                                                  
    }                                                                      
                                                                        
    public int getExecutionTimes() {      
        return 10;
    }                                                          
                                                                     
    public boolean logFullData() {                                  
        return true;                               
    }
                                                                        
    public boolean useKieker() {                                     
        return false;
    }                           
                                              
    public long getMaximalTime() {                       
        return 300000;
    }                                        

    public int getRepetitions() {
        return 1;                             
    }

}
