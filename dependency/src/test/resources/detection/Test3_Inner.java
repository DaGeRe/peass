/**
 * Example comment
 * @author reichelt
 *
 */
class Test3_Inner{
	
	public static final int y = 438;
	public int w = 48;

	/**
	 * Comment 1
	 */
	public Test() {
		// Line-comment
		int a = 3 + 5 - 8;
		System.out.println(a);
	}
	
	/**
	 * Comment 2
	 * @param i
	 */
	public static void doStaticThing(int i){
		int y = i + 1;
		System.out.println(y);
		Runnable r3 = new Runnable(){
		
		@Override
		public void run() {
			System.out.println("Run R3");
			
		}};

		
		r3.run();
	}
	
	public class InnerStuff{
	   
	   class InnerInner{
	       void doubleInnerMethod() {
	          
	       }
	   }
	   public InnerStuff(InnerParameter1 var, InnerParameter2 var2) {
	      System.out.println("This needs to be found!");
	  }
	}
	
}
