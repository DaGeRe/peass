package de.peass;

import java.util.Collection;
import java.util.List;

class C0_0{ 

 public void method0(){
  C1_0 object = new   C1_0();
  object.method0();
  unreachableByException();
  unreachableByLoop();
 }
 
 public void unreachableByException() {
    throw new RuntimeException("Not implemented yet");
 }
 
 public void unreachableByLoop() {
    while (true) {
       System.out.println("I will never end");
    }
 }
 
}
