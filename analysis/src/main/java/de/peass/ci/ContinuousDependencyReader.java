package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.debugtools.DependencyReadingContinueStarter;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

public class ContinuousDependencyReader {

   private static final int TIMEOUT = 10;

   private static final Logger LOG = LogManager.getLogger(ContinuousDependencyReader.class);

   private final String version;
   private final File projectFolder, dependencyFile;

   public ContinuousDependencyReader(String version, File projectFolder, File dependencyFile) {
      this.version = version;
      this.projectFolder = projectFolder;
      this.dependencyFile = dependencyFile;
   }

   Dependencies getDependencies(VersionIterator iterator, String url)
         throws JAXBException, JsonParseException, JsonMappingException, IOException, InterruptedException, XmlPullParserException {
      Dependencies dependencies;
      
      final VersionKeeper noChanges = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonChanges_" + projectFolder.getName() + ".json"));

      if (!dependencyFile.exists()) {
         dependencies = fullyLoadDependencies(url, iterator, noChanges);
      } else {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
         
         if (dependencies.getVersions().size() > 0) {
            final String lastVersionName = dependencies.getVersionNames()[dependencies.getVersions().size() - 1];
            if (!lastVersionName.equals(version)) {
               VersionIterator newIterator = getIterator(lastVersionName);
               DependencyReader reader = new DependencyReader(projectFolder, dependencyFile, dependencies.getUrl(), newIterator, TIMEOUT);
               newIterator.goTo0thCommit();
                
               reader.readCompletedVersions(dependencies);
               reader.readDependencies();
            }
         } else {
            dependencies = fullyLoadDependencies(url, iterator, noChanges);
         }
      }
      VersionComparator.setDependencies(dependencies);
      
      return dependencies;
   }

   public VersionIterator getIterator(final String lastVersionName) {
      GitCommit lastAnalyzedCommit = new GitCommit(lastVersionName, "", "", "");
      GitCommit currentCommit = new GitCommit(version, "", "", "");
      
      List<GitCommit> commits = new LinkedList<>();
      commits.add(lastAnalyzedCommit);
      commits.add(currentCommit);
      VersionIteratorGit newIterator = new VersionIteratorGit(projectFolder, commits, lastAnalyzedCommit);
      return newIterator;
   }

   

   private Dependencies fullyLoadDependencies(final String url, final VersionIterator iterator, final VersionKeeper nonChanges)
         throws IOException, InterruptedException, XmlPullParserException, JsonParseException, JsonMappingException {
      Dependencies dependencies;
      final DependencyReader reader = new DependencyReader(projectFolder, dependencyFile, url, iterator, TIMEOUT, nonChanges);
      iterator.goToPreviousCommit();
      if (!reader.readInitialVersion()) {
         LOG.error("Analyzing first version was not possible");
      } else {
         reader.readDependencies();
      }
      dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      return dependencies;
   }
}
