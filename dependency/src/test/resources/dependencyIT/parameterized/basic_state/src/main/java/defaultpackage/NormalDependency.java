package defaultpackage;

public class NormalDependency {
	public void executeThing(int value) {
	   if (value == 1) {
	      onlyCalledWithOne();
	   }
	}
	
	public void onlyCalledWithOne() {
	   System.out.println("Test");
	}
}
