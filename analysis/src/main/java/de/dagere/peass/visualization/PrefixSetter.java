package de.dagere.peass.visualization;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrefixSetter {
   
   private static final Logger LOG = LogManager.getLogger(NodePreparator.class);
   
   public static void preparePrefix(final GraphNode parent) {
      final String longestPrefix = getLongestPrefix(parent);
      setPrefix(parent, longestPrefix);
      LOG.info("Prefix: {}", longestPrefix);
   }

   private static void setPrefix(final GraphNode parent, final String longestPrefix) {
      parent.setName(parent.getCall().substring(longestPrefix.length()));
      for (final GraphNode node : parent.getChildren()) {
         setPrefix(node, longestPrefix);
      }
   }

   private static String getLongestPrefix(final GraphNode parent) {
      String longestPrefix = parent.getCall();
      for (final GraphNode node : parent.getChildren()) {
         final String clazz = node.getCall().substring(0, node.getCall().lastIndexOf('.') + 1);
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, getLongestPrefix(node), clazz);
      }
      return longestPrefix;
   }

}
