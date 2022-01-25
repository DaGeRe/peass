package de.dagere.peass.dependency.traces;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.requitur.Sequitur;
import de.dagere.requitur.Symbol;

public class PeassSequitur extends Sequitur {
   public void addTraceElements(final List<TraceElement> calls2) {
      addingElements = new LinkedList<>();
      for (final TraceElement element : calls2) {
         final TraceElementContent content = new TraceElementContent(element);
         addingElements.add(content);
         final Symbol symbol = new Symbol(this, content);
         addElement(symbol);
      }
   }
}
