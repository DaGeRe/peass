package de.peass.dependency.parallel;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyParallelReader;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;

public class PartialDependenciesMerger {

   private static final Logger LOG = LogManager.getLogger(DependencyParallelReader.class);

   private PartialDependenciesMerger() {

   }

   public static Dependencies mergeVersions(final File out, final File[] partFiles) throws IOException, JsonGenerationException, JsonMappingException {
      final List<Dependencies> deps = readDependencies(partFiles);
      Dependencies merged = mergeDependencies(deps);

      Constants.OBJECTMAPPER.writeValue(out, merged);
      return merged;
   }

   static List<Dependencies> readDependencies(final File[] partFiles) {
      final List<Dependencies> deps = new LinkedList<>();
      for (int i = 0; i < partFiles.length; i++) {
         try {
            LOG.debug("Reading: {}", partFiles[i]);
            final Dependencies currentDependencies = Constants.OBJECTMAPPER.readValue(partFiles[i], Dependencies.class);
            deps.add(currentDependencies);
            LOG.debug("Size: {}", deps.get(deps.size() - 1).getVersions().size());
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
      return deps;
   }

   public static Dependencies mergeDependencies(final List<Dependencies> deps) {
      LOG.debug("Sorting {} dependencies", deps.size());
      deps.sort(new Comparator<Dependencies>() {
         @Override
         public int compare(final Dependencies o1, final Dependencies o2) {
            final int indexOf = VersionComparator.getVersionIndex(o1.getInitialversion().getVersion());
            final int indexOf2 = VersionComparator.getVersionIndex(o2.getInitialversion().getVersion());
            return indexOf - indexOf2;
         }
      });
      Dependencies merged = deps.get(0);
      if (deps.size() > 1) {
         for (int i = 1; i < deps.size(); i++) {
            final Dependencies newMergeDependencies = deps.get(i);
            LOG.debug("Merge: {} Vals: {}", i, newMergeDependencies.getVersionNames());
            if (newMergeDependencies != null) {
               merged = DependencyReaderUtil.mergeDependencies(merged, newMergeDependencies);
            }
         }
      }
      return merged;
   }
}
