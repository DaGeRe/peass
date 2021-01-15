package de.peass;

class C0_0{ 
 public C0_0(){
   super();
 }
 
 public C0_0(int i){
   this();
   System.out.println("Test");
 }

 public void method0(){
  C1_0 object = new   C1_0();
  object.method0();
 }
 
 void methodPublicVisibility() {
 
 }
 
 private void privateMethod() {
 
 }
 
 public void method0(int i){
   method0("Test");
   i++;
   new Runnable(){
       public void run(){
         System.out.println("Regular anonymous class call");
       }
   }.run();
 }
 
 public String method0(String i){
   myStaticStuff();
   return i;
 }
 
 public static void myStaticStuff(){
   new MyInnerClass(25).innerMethod();
   new Runnable(){
       public void run(){
         System.out.println("Anonymous class call inside static method");
       }
   }.run();
 }
 
 static class MyInnerClass {
   
   public MyInnerClass(int i){
     new Runnable(){
       public void run(){
         System.out.println("Anonymous class call inside inner constructor");
       }
     }.run();
   }
   
   public void innerMethod(){
   
   }
 }
}
