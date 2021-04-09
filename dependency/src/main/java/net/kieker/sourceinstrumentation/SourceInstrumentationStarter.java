package net.kieker.sourceinstrumentation;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.Callable;

import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class SourceInstrumentationStarter implements Callable<Integer>{
   
   @Option(names = { "-folder", "--folder" }, description = "Folder where files should be instrumented", required = true)
   private File projectFolder;
   
   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new SourceInstrumentationStarter());
      commandLine.execute(args);
   }

   @Override
   public Integer call() throws Exception {
      final HashSet<String> includedPatterns = new HashSet<>();
      includedPatterns.add("*");
      final InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, true, false, includedPatterns, true, 1000);
      final InstrumentKiekerSource sourceInstrumenter = new InstrumentKiekerSource(configuration);
      sourceInstrumenter.instrumentProject(projectFolder);;
      return 0;
   }
}
