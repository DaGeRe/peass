package de.peran.debugtools;

import java.io.File;

import de.peran.dependency.analysis.CalledMethodLoader;

public class OnyLoad {
   public static void main(String[] args) {
      final File kiekerFolder = new File(args[0]);
      final File projectFolder = new File("../../projekte/commons-io/");
      
      final CalledMethodLoader loader = new CalledMethodLoader(kiekerFolder, projectFolder);
      System.out.println(loader.getCalledMethods());
   }
}
