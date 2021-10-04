package de.dagere.peass;

import java.util.Random;

/**
 * Adds random numbers
 * 
 * @author reichelt
 *
 */
public class AddRandomNumbers {

   private static final Random RANDOM = new Random();

   int x = 0;

   public int doSomething(int count) {
      for (int i = 0; i < count; i++)
         x += RANDOM.nextInt(100);
      return x;
   }

   public int getValue() {
      return x;
   }
}
