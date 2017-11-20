package defaultpackage;

public class NormalDependency {
	public void executeThing() {

	}

	public void unusedMethod() {

	}
	
	/**
	 * An added method, i.e. an class change, is introduced to test whether all tests using the whole class are called.
	 */
	public void iAmAnClassChange(){
		
	}
}
