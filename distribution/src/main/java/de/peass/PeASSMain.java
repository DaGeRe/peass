package de.peass;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "peass", mixinStandardHelpOptions = true, subcommands = { DependencyExecutionReader.class, AdaptiveTestStarter.class,
      GetChanges.class }, synopsisSubcommandLabel = "COMMAND")
public class PeASSMain implements Callable<Void> {
   public static void main(String[] args) {
      CommandLine line = new CommandLine(new PeASSMain());
      if (args.length != 0) {
         line.execute(args);
      } else {
         line.usage(System.out);
      }

   }

   @Override
   public Void call() throws Exception {
      return null;
   }
}
