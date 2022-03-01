package de.dagere.peass.dependency.traces.coverage;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependency.traces.OneTraceGenerator;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.utils.Constants;

public class CoverageSelectionExecutor {

   private static final Logger LOG = LogManager.getLogger(CoverageSelectionExecutor.class);

   private final TraceFileMapping mapping;
   private final ExecutionData coverageBasedSelection;
   private final CoverageSelectionInfo coverageSelectionInfo;

   public CoverageSelectionExecutor(TraceFileMapping mapping, ExecutionData coverageBasedSelection, CoverageSelectionInfo coverageSelectionInfo) {
      this.mapping = mapping;
      this.coverageBasedSelection = coverageBasedSelection;
      this.coverageSelectionInfo = coverageSelectionInfo;
   }

   public void generateCoverageBasedSelection(final String version, final VersionStaticSelection newVersionInfo, TestSet dynamicallySelected)
         throws IOException, JsonParseException, JsonMappingException {
      List<TraceCallSummary> summaries = getSummaries(dynamicallySelected);

      for (ChangedEntity change : newVersionInfo.getChangedClazzes().keySet()) {
         LOG.info("Change: {}", change.toString());
         LOG.info("Parameters: {}", change.getParametersPrintable());
      }

      CoverageSelectionVersion selected = CoverageBasedSelector.selectBasedOnCoverage(summaries, newVersionInfo.getChangedClazzes().keySet());
      for (TraceCallSummary traceCallSummary : selected.getTestcases().values()) {
         if (traceCallSummary.isSelected()) {
            coverageBasedSelection.addCall(version, traceCallSummary.getTestcase());
         }
      }
      coverageSelectionInfo.getVersions().put(version, selected);
   }

   private List<TraceCallSummary> getSummaries(TestSet dynamicallySelected) throws IOException, StreamReadException, DatabindException {
      List<TraceCallSummary> summaries = new LinkedList<>();
      for (TestCase testcase : dynamicallySelected.getTests()) {
         List<File> traceFiles = mapping.getTestcaseMap(testcase);
         if (traceFiles != null && traceFiles.size() > 1) {
            File oldFile = new File(traceFiles.get(0).getAbsolutePath() + OneTraceGenerator.SUMMARY);
            File newFile = new File(traceFiles.get(1).getAbsolutePath() + OneTraceGenerator.SUMMARY);
            TraceCallSummary oldSummary = Constants.OBJECTMAPPER.readValue(oldFile, TraceCallSummary.class);
            TraceCallSummary newSummary = Constants.OBJECTMAPPER.readValue(newFile, TraceCallSummary.class);
            summaries.add(oldSummary);
            summaries.add(newSummary);
            LOG.info("Found traces for {}", testcase);
         } else {
            LOG.info("Trace files missing for {}", testcase);
         }
      }
      return summaries;
   }
}
