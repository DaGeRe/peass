package de.dagere.peass.ci;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionIteratorGit;

public class IteratorBuilder {
   private final String version, versionOld;
   private final VersionIteratorGit iterator;
   
   public IteratorBuilder(final MeasurementConfiguration measurementConfig, final File projectFolder) {
      versionOld = GitUtils.getName(measurementConfig.getVersionOld() != null ? measurementConfig.getVersionOld() : "HEAD~1", projectFolder);
      version = GitUtils.getName(measurementConfig.getVersion() != null ? measurementConfig.getVersion() : "HEAD", projectFolder);

      final List<GitCommit> entries = new LinkedList<>();
      final GitCommit prevCommit = new GitCommit(versionOld, "", "", "");
      entries.add(prevCommit);
      entries.add(new GitCommit(version, "", "", ""));
      iterator = new VersionIteratorGit(projectFolder, entries, prevCommit);
   }

   public String getVersion() {
      return version;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public VersionIteratorGit getIterator() {
      return iterator;
   }
   
   
}
