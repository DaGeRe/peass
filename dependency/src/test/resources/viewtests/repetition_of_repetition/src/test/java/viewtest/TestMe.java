package viewtest;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.junit.testrunner.PerformanceTestRunnerJUnit;

@RunWith(PerformanceTestRunnerJUnit.class)
public class TestMe {
	
	class InnerClass {
		public InnerClass(){ 
			System.out.println("Constructor");
		}
		
		public void method(){
			System.out.println("Method");
		}
	}

	public static void staticMethod(){
		System.out.println("Static Method");
	}
	
	@Test
	@PerformanceTest(executionTimes=1, warmupExecutions=0, useKieker=true)
	public void test() {
		InnerClass c = new InnerClass();
		for (int j = 0; j < 3; j++){
			for (int i = 0; i < 5; i++){
				c.method();
				staticMethod();
			}
			staticMethod();
		}
	}
}
