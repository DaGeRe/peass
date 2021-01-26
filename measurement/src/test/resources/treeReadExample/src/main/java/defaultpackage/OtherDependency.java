package defaultpackage;

class OtherDependency {
   public void executeThing() {
      for (int i = 0; i < 3; i++) {
         child1();
         child2();
         child3();
      }
   }

   public void other() {

   }

   public void unusedMethod() {

   }

   public void child1() {
      for (int i = 0; i < 3; i++)
         child3();
   }

   public void child2() {
      for (int i = 0; i < 3; i++) {
         child12();
         child3();
      }
   }

   public void child12() {
      for (int i = 0; i < 10; i++)
         child13();
   }

   public void child13() {

   }

   public void child3() {
      child31();
   }

   public void child31() {
   }
}
