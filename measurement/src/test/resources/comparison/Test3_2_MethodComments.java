/**
 * Example comment with minor change
 * @author reichelt
 *
 */
class Test{
	
	public static final int y = 438;
	public int w = 48;

	/**
	 * Comment 1 with minor change
	 */
	public Test() {
		// Line-comment
		int a = 3 + 5 - 8;
		System.out.println(a);
	}
	
	public static void doStaticThing(int i){
		int y = i + 1;
		System.out.println(y);
	}
	
	/**
	 * Comment 3
	 * @param this param never has been there
	 */
	public void doNonStaticThing(){
		System.out.println("a");
	}
}