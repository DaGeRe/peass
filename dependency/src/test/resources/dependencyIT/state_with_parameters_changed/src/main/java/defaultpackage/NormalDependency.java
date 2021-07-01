package defaultpackage;

public class NormalDependency {
	public void executeThing() {
		innerMethod(5);
	}
	
	public void innerMethod(Integer param){
		System.out.println("This is some added behaviour");
	}

	public void unusedMethod() {

	}
}
