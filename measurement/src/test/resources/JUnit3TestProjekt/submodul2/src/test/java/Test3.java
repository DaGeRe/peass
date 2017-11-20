import junit.framework.TestCase;

public class Test3 extends TestCase{
	public Test3(){
		
	}
	
	public void testWas3(){
		int i = 0;
		while (i < 300){
			i+=2;
		}
		System.out.println("Das ist ein Test: "+ i);
	}
	
	public void testWas4(){
		int i = 0;
		while (i < 400){
			i+=2;
		}
		System.out.println("Das ist ein Test: "+ i);
	}
}