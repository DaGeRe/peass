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


import java.io.File;

import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.writer.filesystem.ChangeableFolderSyncFsWriter;

public class CallerSimpleFor {

    int x = 0;

    public static void main(final String[] args) {
		try {
			final File tmpFolder = new File("target/kieker_results_test/");
			ChangeableFolderSyncFsWriter.getInstance(MonitoringController.getInstance()).setFolder(tmpFolder);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        System.out.println("Here it starts");
        final CallerSimpleFor c = new CallerSimpleFor();
        c.method1();
        c.method3();
    }

    public void method1() {
        x++;
        method2();
        method2(x+1);
        main(3);
        main("asd");
    }
    
    private void main(final int z){
    	System.out.println("Zweite main!");
    }
    
    private void main(final String z){
    	System.out.println("Dritte main!");
    }
    
    private void method2() {
        System.out.println("X has been incremented: " + x);
    }

    private void method2(final int z) {
        System.out.println("X has been incremented and z has been defined " + z);
    }

    public void method3() {
        System.out.println("Lets try another class");
        final CalleeSimpleFor c = new CalleeSimpleFor();
        c.callMe();
    }

    public void methodNever() {
        System.out.println("I  am never called");
    }
}
