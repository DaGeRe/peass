package defaultpackage;

public class NormalDependency {
	static int i = 0;
	public void executeThing() {
		if (i != 0){
			throw new RuntimeException("i is not 0!");
		}
		i++;
	}
}
