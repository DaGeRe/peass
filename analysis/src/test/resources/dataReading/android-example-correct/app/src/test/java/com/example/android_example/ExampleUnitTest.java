package com.example.android_example;

import org.junit.Test;

import org.junit.Assert;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void test_TestMe(){
        TestMe t = new TestMe();
        // t.test() always returns 1, test will always pass
        Assert.assertNotEquals(0, t.test());
    }
}
