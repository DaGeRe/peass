package de.dagere.peass.dependencyprocessors;

import java.io.File;

public interface KiekerResultHandler{
   
   /**
    * This method can be overriden in order to handle kieker results before they are compressed
    * 
    * @param folder
    */
   void handleKiekerResults(final String version, final File folder);
}