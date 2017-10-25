import junit.framework.TestCase;

public class Test2 extends TestCase{
	public Test2(){
		
	}
	
	public void testWas2(){
		int i = 0;
		while (i < 200){
			i+=2;
		}
		System.out.println("Das ist ein Test: "+ i);
		
	}
}