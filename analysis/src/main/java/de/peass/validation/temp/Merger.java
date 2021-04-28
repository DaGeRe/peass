package de.peass.validation.temp;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;
import de.dagere.peass.utils.Constants;

public class Merger {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File source = new File(args[0]);
      final File source2 = new File(args[1]);

      final CauseSearchData first = Constants.OBJECTMAPPER.readValue(source, CauseSearchData.class);
      final CauseSearchData second = Constants.OBJECTMAPPER.readValue(source2, CauseSearchData.class);

      final MeasuredNode firstNode = first.getNodes();
      final MeasuredNode secondNode = second.getNodes();

      merge(firstNode, secondNode);

      final File mergedFile = new File(source.getParentFile(), "merged.json");
      Constants.OBJECTMAPPER.writeValue(mergedFile, first);
   }

   private static void merge(final MeasuredNode firstNode, final MeasuredNode secondNode) {
      for (int i = 0; i < firstNode.getChildren().size(); i++) {
         MeasuredNode child1 = firstNode.getChildren().get(i);
         MeasuredNode child2 = secondNode.getChildren().get(i);
         if (!child1.getKiekerPattern().equals(child1.getKiekerPattern())) {
            System.out.println("Appending2: " + child2);
            firstNode.getChildren().add(child2);
         } else {
            merge(child1, child2);
         }
      }
      System.out.println("Children: " + secondNode.getChildren().size() + " " + firstNode.getChildren().size());
      if (secondNode.getChildren().size() > firstNode.getChildren().size()) {
         for (int i = firstNode.getChildren().size(); i < secondNode.getChildren().size(); i++) {
            final MeasuredNode appendChild = secondNode.getChildren().get(i);
            System.out.println("Appending: " + appendChild.getKiekerPattern());
            firstNode.getChildren().add(appendChild);
         }
      }
   }
}
