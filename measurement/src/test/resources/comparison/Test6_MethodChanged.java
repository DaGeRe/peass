/**
 * Example comment
 * @author reichelt
 *
 */
class Test{
	
	public static final int y = 438;
	public int w = 48;

	/**
	 * Comment 1
	 */
	public Test() {
		// Line-comment
		int a = 3 + 5 - 8;
		System.out.println(a);
		
		int b = 3 + a;
		System.out.println(b);
	}
	
	/**
	 * Comment 2
	 * @param i
	 */
	public static void doStaticThing(int i){
		int y = i + 1;
		System.out.println(y);
	}
	
	/**
	 * Comment 3
	 */
	public void doNonStaticThing(){
		System.out.println("a");
		
		int z = 5;
		System.out.println(z);
	}
}