package de.dagere.peass.measurement.rca.kiekerReading;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kieker.record.DurationRecord;
import de.dagere.kopeme.kopemedata.MeasuredValue;
import kieker.analysis.trace.AbstractTraceAnalysisStage;
import kieker.model.repository.SystemModelRepository;

public class DurationMeasurementStage extends AbstractTraceAnalysisStage<DurationRecord> {

   private final List<MeasuredValue> values = new LinkedList<>();

   /**
    * Creates a new instance of this class using the given parameters.
    *
    * @param repository system model repository
    */
   public DurationMeasurementStage(final SystemModelRepository systemModelRepository) {
      super(systemModelRepository);
   }

   @Override
   protected void execute(final DurationRecord execution) throws Exception {
      MeasuredValue value = new MeasuredValue();
      value.setStartTime(execution.getTin());
      value.setValue(execution.getTout() - execution.getTin());
      values.add(value);
   }
   
   public List<MeasuredValue> getValues() {
      return values;
   }
}
