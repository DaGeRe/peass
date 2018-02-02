package de.peran.traceminimization;

import org.junit.Assert;
import org.junit.Test;

import de.peran.dependency.analysis.data.TraceElement;
import de.peran.dependency.traces.requitur.content.TraceElementContent;

public class TestTraceElementContent {
	
	@Test
	public void testEquality(){
		TraceElement element = new TraceElement("Clazz", "method", 1);
		element.setParameterTypes(new String[]{"String"});
		TraceElementContent content1 = new TraceElementContent(element);
		TraceElement element2 = new TraceElement("Clazz", "method", 1);
		element2.setParameterTypes(new String[]{"String[]"});
		TraceElementContent content2 = new TraceElementContent(element2);
		
		Assert.assertFalse(content1.equals(content2));
	}
}
