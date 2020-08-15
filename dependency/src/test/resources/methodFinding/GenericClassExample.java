
public class GenericClassExample<T extends Comparable> {

	public void test1(){}

	public T myMethod(T thing){
		Thread myStuff = new Thread(new Runnable() {
			
			@Override
			public void run() {
				System.out.println("I'd like to be found");
			}
		});
	}
}
