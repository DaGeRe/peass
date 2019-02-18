
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
		
		public MyPrivateClass(int secondConstructorParameter) {
         System.out.println("Parameter: " + secondConstructorParameter);
      }
		
		public void doSomething(){
			System.out.println("My method");
		}
	}

	public int parameterMethod(int a){

	}

	public int parameterMethod(String a){

	}

	public int parameterMethod(Object a, String... b){

	}
}
