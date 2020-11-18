package de.test;

/**
 * Hello world!
 *
 */
public class Callee {
   public Callee() {
      System.out.println("Constructor");
   }

   public void method1() {
      innerMethod();
   }

   private void innerMethod() {
      try {
         Thread.sleep(20);
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }

   public void method2() {
      // This change should not be detected by PeASS since it is not covered by a test
      System.out.println("This is a test");
   }
}
