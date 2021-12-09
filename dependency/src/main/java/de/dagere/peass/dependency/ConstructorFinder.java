package de.dagere.peass.dependency;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class ConstructorFinder {
   public static Constructor<?> findConstructor(final Class<?> executorClazz) {
      Constructor<?> constructor = null;
      for (Constructor<?> candidate : executorClazz.getConstructors()) {
         Parameter[] parameters = candidate.getParameters();
         if (parameters.length == 3) {
            if (isSubclass(parameters[0].getType(), PeassFolders.class) &&
                  isSubclass(parameters[1].getType(), TestTransformer.class) &&
                  isSubclass(parameters[2].getType(), EnvironmentVariables.class)) {
               constructor = candidate;
            }
         }
      }
      if (constructor == null) {
         throw new RuntimeException("No constructor with PeassFolders, TestTransformer and EnvironmentVariables-parameters found");
      }
      return constructor;
   }

   static boolean isSubclass(final Class<?> checkedClazz, final Class<?> targetClass) {
      boolean isSubclass = checkSubclassOrInterface(targetClass, checkedClazz);
      Class<?> superclass = checkedClazz;
      System.out.println("Testing " + superclass + " " + superclass.isInterface());
      while (!superclass.equals(Object.class) && !superclass.isInterface()) {
         superclass = superclass.getSuperclass();
         boolean isSubclassOrInterface = checkSubclassOrInterface(targetClass, superclass);
         if (isSubclassOrInterface) {
            isSubclass = true;
            break;
         }
      }
      System.out.println(checkedClazz + " " + isSubclass);
      return isSubclass;
   }

   private static boolean checkSubclassOrInterface(final Class<?> targetClass, final Class<?> superclass) {
      List<Class<?>> interfaces = Arrays.asList(superclass.getInterfaces());
      System.out.println(superclass + " " + interfaces);
      boolean isSubclassOrInterface = superclass.equals(targetClass) || interfaces.contains(targetClass);
      return isSubclassOrInterface;
   }
}
