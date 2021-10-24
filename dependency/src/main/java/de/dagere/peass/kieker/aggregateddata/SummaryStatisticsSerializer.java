package de.dagere.peass.kieker.aggregateddata;

import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class SummaryStatisticsSerializer extends StdSerializer<StatisticalSummary> {

   public SummaryStatisticsSerializer() {
      super(StatisticalSummary.class);
   }

   private static final long serialVersionUID = 6773506005705287342L;

   @Override
   public void serialize(final StatisticalSummary value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
      if (value.getN() != 0) {
         gen.writeStartObject();
         gen.writeFieldName("mean");
         gen.writeNumber(value.getMean());
         gen.writeFieldName("standardDeviation");
         gen.writeNumber(value.getStandardDeviation());
         gen.writeFieldName("n");
         gen.writeNumber(value.getN());
         gen.writeFieldName("min");
         gen.writeNumber(value.getMin());
         gen.writeFieldName("max");
         gen.writeNumber(value.getMax());
         gen.writeEndObject();
      } else {
         gen.writeString("NO_STATISTICS");
      }
   }

}