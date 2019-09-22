package defaultpackage;

class Superclass{
	public void superMethod(){}
	public void moveMethod(){}
}

public class NormalDependency extends Superclass{
	public void executeThing() {
		child1();
		child2();
		child3();
		superMethod();
		moveMethod();
	}

	public void other() {
		
	}

	public void unusedMethod() {

	}

	public void child1(){
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
