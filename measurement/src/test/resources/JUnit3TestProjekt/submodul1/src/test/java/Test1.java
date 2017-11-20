import junit.framework.TestCase;

public class Test1 extends TestCase{
	public Test1(){
		
	}
	
	public void testWas(){
		int i = 0;
		while (i < 100){
			i+=2;
		}
		System.out.println("Das ist ein Test: "+ i);
		
	}
}