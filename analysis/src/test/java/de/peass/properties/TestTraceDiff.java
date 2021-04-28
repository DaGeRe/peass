package de.peass.properties;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.analysis.properties.ChangeProperty;
import de.dagere.peass.analysis.properties.ChangeProperty.TraceChange;
import de.dagere.peass.analysis.properties.PropertyReadHelper;

public class TestTraceDiff {
   
   
   final List<String> trace1 = Arrays.asList(new String[] {"m1", "m2", "m2", "m4"});
   final List<String> trace2 = Arrays.asList(new String[] {"m1", "m2", "m2", "m3", "m4"});
   final List<String> trace3 = Arrays.asList(new String[] {"m1", "m2", "m2", "m5", "m4"});
   
   @Test
   public void testAdd() throws IOException {
      final ChangeProperty property = new ChangeProperty();
      PropertyReadHelper.determineTraceSizeChanges(property, trace2, trace1);
      Assert.assertEquals(property.getTraceChangeType(), TraceChange.ADDED_CALLS);
      Assert.assertEquals(property.getCallsOld(), 4);
      Assert.assertEquals(property.getCalls(), 5);
   }
   
   @Test
   public void testRemove() throws IOException {
      final ChangeProperty property = new ChangeProperty();
      PropertyReadHelper.determineTraceSizeChanges(property, trace1, trace2);
      Assert.assertEquals(property.getTraceChangeType(), TraceChange.REMOVED_CALLS);
      Assert.assertEquals(property.getCallsOld(), 5);
      Assert.assertEquals(property.getCalls(), 4);
   }
   
   @Test
   public void testBoth() throws IOException {
      final ChangeProperty property = new ChangeProperty();
      PropertyReadHelper.determineTraceSizeChanges(property, trace2, trace3);
      Assert.assertEquals(property.getTraceChangeType(), TraceChange.BOTH);
      Assert.assertEquals(property.getCallsOld(), 5);
      Assert.assertEquals(property.getCalls(), 5);
      
   }
}
