package defaultpackage;

public class NormalDependency {
   public void executeThing() {
      child1();
      child2();
      child3();
   }

   public void other() {

   }

   public void unusedMethod() {

   }

   public void child1() {
      child12();
      child13();
   }

   public void child2() {

   }

   public void child12() {
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public void child13() {

   }

   public void child3() {
      child31();
   }

   public void child31() {
   };
}
