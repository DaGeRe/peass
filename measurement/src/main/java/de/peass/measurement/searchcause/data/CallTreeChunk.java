package de.peass.measurement.searchcause.data;

import java.util.LinkedList;
import java.util.List;

class CallTreeChunk {
   final List<Long> values = new LinkedList<>();

   public List<Long> getValues() {
      return values;
   }

   public void addValue(Long value) {
      values.add(value);
   }
}