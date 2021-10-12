package net.kieker.sourceinstrumentation.instrument;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kieker.monitoring.core.signaturePattern.InvalidPatternException;
import kieker.monitoring.core.signaturePattern.PatternParser;

public class SignatureMatchChecker {

   private static final Logger LOG = LogManager.getLogger(FileInstrumenter.class);

   private Set<String> includes;
   private Set<String> excludes;

   public SignatureMatchChecker(final Set<String> includes, final Set<String> excludes) {
      this.includes = includes;
      this.excludes = excludes;
   }

   public boolean testSignatureMatch(final String signature) {
      boolean oneMatches = false;
      if (includes == null) {
         oneMatches = true;
      } else {
         for (String pattern : includes) {
            pattern = fixConstructorPattern(pattern);
            try {
               Pattern patternP = PatternParser.parseToPattern(pattern);
               if (patternP.matcher(signature).matches()) {
                  oneMatches = true;
                  break;
               }
            } catch (InvalidPatternException e) {
               LOG.error("Wrong pattern: {}", pattern);
               throw new RuntimeException(e);
            }
         }
      }
      if (excludes != null) {
         for (String pattern : excludes) {
            pattern = fixConstructorPattern(pattern);
            try {
               Pattern patternP = PatternParser.parseToPattern(pattern);
               if (patternP.matcher(signature).matches()) {
                  oneMatches = false;
                  break;
               }
            } catch (InvalidPatternException e) {
               LOG.error("Wrong pattern: {}", pattern);
               throw new RuntimeException(e);
            }
         }
      }

      return oneMatches;
   }

   /**
    * In Kieker 1.14, the return type new is ignored for pattern. Therefore, * needs to be set as return type of constructors in pattern.
    */
   private String fixConstructorPattern(String pattern) {
      if (pattern.contains("<init>")) {
         final String[] tokens = pattern.substring(0, pattern.indexOf('(')).trim().split("\\s+");
         int returnTypeIndex = 0;
         String modifier = "";
         if (tokens[0].equals("private") || tokens[0].equals("public") || tokens[0].equals("protected")) {
            returnTypeIndex++;
            modifier = tokens[0];
         }
         final String returnType = tokens[returnTypeIndex];
         if (returnType.equals("new")) {
            String patternChanged = modifier + " *" + pattern.substring(pattern.indexOf("new") + 3);
            LOG.trace("Changing pattern {} to {}, since Kieker 1.14 does not allow pattern with new", pattern, patternChanged);
            pattern = patternChanged;
         }
      }
      return pattern;
   }
}
