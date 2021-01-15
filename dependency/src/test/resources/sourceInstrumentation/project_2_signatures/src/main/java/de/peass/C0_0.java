package de.peass;

import java.util.List;

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
   methodWithArrayParam(null, null, null);
 }
 
 public String[] methodWithArrayParam(byte[] myBytes, int[] myInts, String[] myStrings) {
    return null;
 }
 
 public List[] secondMethodWithArrayParam(byte[] myBytes, int[] myInts, List[] myStrings) {
    return null;
 }
 
 public C0_0 doSomethingWithSamePackageObject(C1_0 other) {
    return null;
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
 
 public MyInnerClass2 getInnerInstance() {
    return null;
 }
 
 static class MyInnerClass2 {
    
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
