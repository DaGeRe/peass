package de.peass.dependency.traces.requitur;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.TraceElement;
import de.peass.dependency.traces.requitur.content.Content;
import de.peass.dependency.traces.requitur.content.StringContent;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

public class Sequitur {

   private static final Logger LOG = LogManager.getLogger(Sequitur.class);

   Map<Digram, Digram> digrams = new HashMap<>();
   Map<String, Rule> rules = new HashMap<>();
   List<Rule> ununsedRules = new LinkedList<>();
   private Symbol startSymbol = new Symbol(this, (StringContent) null);
   Symbol lastSymbol = startSymbol;
   private int ruleindex = 0;

   Digram link(final Symbol start, final Symbol end) {
      start.setSucessor(end);
      end.setPredecessor(start);
      if (start.getValue() != null && end.getValue() != null) {
         final Digram newDigram = new Digram(start, end);
         handleDigram(newDigram);
         return newDigram;
      } else {
         return null;
      }
   }

   public void addElement(final Symbol symbol) {
      // TraceStateTester.assureCorrectState(this);
      if (startSymbol == null) {
         startSymbol = symbol;
         lastSymbol = symbol;
      } else {
         lastSymbol.setSucessor(symbol);
         symbol.setPredecessor(lastSymbol);
         lastSymbol = symbol;
         if (symbol.getPredecessor().getValue() != null) {
            final Digram digram = new Digram(symbol.getPredecessor(), symbol);
            handleDigram(digram);
         }
      }
      // TraceStateTester.assureCorrectState(this);
   }

   void handleDigram(final Digram digram) {
      final Digram existing = digrams.get(digram);
      if (existing != null) {
         if (digram.getStart() != existing.getEnd()) {
            if (existing.rule != null) {
               existing.rule.use(digram);
            } else {
               Rule rule;
               if (ununsedRules.size() > 0) {
                  rule = ununsedRules.remove(0);
                  rule.setDigram(existing);
               } else {
                  // if (digram.getStart().isRule() && digram.getStart().getRule().getUsage() == 2) {
                  // rule = digram.getStart().getRule();
                  // link(rule.getAnchor().getPredecessor(), digram.getEnd());
                  // link(existing.getStart(), existing.getEnd().getSucessor());
                  // } else {
                  rule = new Rule(this, ruleindex, existing);
                  ruleindex++;
                  // }
               }
               rules.put(rule.getName(), rule);

               rule.use(digram);
            }
         }
      } else {
         digrams.put(digram, digram);
      }
   }

   public List<Content> getTrace() {
      Symbol iterator = startSymbol.getSucessor();
      final List<Content> trace = new LinkedList<>();
      while (iterator != null) {
         trace.add(iterator.getValue());
         iterator = iterator.getSucessor();
      }
      return trace;
   }

   public List<Content> getUncompressedTrace() {
      Symbol iterator = startSymbol.getSucessor();
      final List<Content> trace = new LinkedList<>();
      while (iterator != null) {
         for (int i = 0; i < iterator.getOccurences(); i++) {
            trace.add(iterator.getValue());
         }
         iterator = iterator.getSucessor();
      }
      return trace;
   }

   public Map<String, Rule> getRules() {
      return rules;
   }

   @Override
   public String toString() {
      return getTrace().toString();
   }

   List<Content> addingElements;

   public void addElements(final List<String> mytrace) {
      addingElements = new LinkedList<>();
      for (final String element : mytrace) {
         addingElements.add(new StringContent(element));
         final Symbol symbol = new Symbol(this, new StringContent(element));
         addElement(symbol);
      }
   }

   public void addTraceElements(final List<TraceElement> calls2) {
      addingElements = new LinkedList<>();
      for (final TraceElement element : calls2) {
         final TraceElementContent content = new TraceElementContent(element);
         addingElements.add(content);
         final Symbol symbol = new Symbol(this, content);
         addElement(symbol);
      }
   }

   public Symbol getStartSymbol() {
      return startSymbol;
   }

   public static List<String> getExpandedTrace(final File methodTraceFile) throws IOException, FileNotFoundException {
      final List<String> trace1 = new LinkedList<>();
      try (BufferedReader br = new BufferedReader(new FileReader(methodTraceFile))) {
         String line;
         while ((line = br.readLine()) != null) {
            final List<String> elements = getCurrentValues(line, br).elements;
            final List<String> added = expandTraceElements(elements);
            trace1.addAll(added);
         }
      }
      return trace1;
   }

   public static List<String> expandTraceElements(final List<String> elements) {
      final List<String> added = new LinkedList<>();
      for (final String element : elements) {
         if (element.contains("(")) {
            final String parameters = element.substring(element.indexOf("(") + 2, element.length() - 2);
            final String[] splitted = parameters.split(",");
            String withoutFQN = "(";
            for (final String parameter : splitted) {
               withoutFQN += parameter.substring(parameter.lastIndexOf('.') + 1).trim() + ",";
            }
            withoutFQN = withoutFQN.substring(0, withoutFQN.length() - 1) + ")";
            added.add(element.substring(0, element.indexOf("(")) + withoutFQN);
         } else {
            added.add(element);
         }
      }
      return added;
   }

   static class Return {
      int readLines = 1;
      List<String> elements = new LinkedList<>();
   }

   public static Return getCurrentValues(String line, final BufferedReader reader) throws IOException {
      final Return current = new Return();
      final String trimmedLine = line.trim();
      if (line.matches("[ ]*[0-9]+ x [#]?[0-9]* \\([0-9]+\\)")) {
         final String[] parts = trimmedLine.split(" ");
         final int count = Integer.parseInt(parts[0]);
         final int length = Integer.parseInt(parts[3].replaceAll("[\\(\\)]", ""));
         final List<String> subList = new LinkedList<>();
         for (int i = 0; i < length;) {
            line = reader.readLine();
            final Return lines = getCurrentValues(line, reader);
            current.readLines += lines.readLines;
            i += lines.readLines;
            subList.addAll(lines.elements);
         }
         for (int i = 0; i < count; i++) {
            current.elements.addAll(subList);
         }
      } else if (line.matches("[ ]*[0-9]+ x .*$")) {
         final String method = trimmedLine.substring(trimmedLine.indexOf("x") + 2);
         final String countString = trimmedLine.substring(0, trimmedLine.indexOf("x") - 1);
         final int count = Integer.parseInt(countString);
         for (int i = 0; i < count; i++) {
            current.elements.add(method);
         }
      } else if (line.matches("[ ]*[#]?[0-9]* \\([0-9]+\\)")) {
         // Do nothing - just info element, that same trace pattern occurs twice
      } else {
         current.elements.add(trimmedLine);

      }
      return current;
   }

}
