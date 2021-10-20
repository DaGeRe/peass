package de.peass;

import java.util.Comparator;

enum MyEnum {
 PETER("Peter"), HEIDI("Heidi");

 String name;
   
 MyEnum(String name) {
  this.name = name;
 }

 public String getName() {
  return name;
 }
}


class C0_0{ 
 
 public C0_0() {
   String value = MyEnum.PETER.getName();
 }
   
 public void method0(String signature){
 }
}
