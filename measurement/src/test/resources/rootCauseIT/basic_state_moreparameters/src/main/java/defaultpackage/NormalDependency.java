package defaultpackage;

public class NormalDependency {
	public void executeThing() {
		child1(new int[0], 1.0, "Test");
		child2();
		child3();
	}

	public void other() {
		
	}

	public void unusedMethod() {

	}

	public void child1(int[] valueOne, double valueTwo, String valueThree){
		child12();
		child13();
	}

	public void child2(){

	}

	public void child12(){
		
	}
	public void child13(){

	}

	public void child3(){child31();}
	public void child31(){};
}
