
public class AnonymousClassExample {
	public void myMethod(){
		Thread myStuff = new Thread(new Runnable() {
			
			@Override
			public void run() {
				System.out.println("I'd like to be found");
			}
		});
	}
	
	class MyPrivateClass{
		public MyPrivateClass(){
			System.out.println("Constructor!");
		}
		
		public void doSomething(){
			System.out.println("My method");
		}
	}
}
