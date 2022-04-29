package de.dagere.peass.execution.utils;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Dependency;

import de.dagere.peass.execution.maven.pom.MavenPomUtil;


public class RequiredDependency {

   private final String groupId;
   private final String artifactId;
   private final String version;
   private final String scope;
   private final String classifier;

   public RequiredDependency(final String groupId, final String artifactId, final String version, final String scope, final String classifier) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.scope = scope;
      this.classifier = classifier;
   }

   public static List<RequiredDependency> getAll(final boolean isJUnit3) {
      List<RequiredDependency> all = new LinkedList<RequiredDependency>();

      if (isJUnit3) {
         all.add(new RequiredDependency("de.dagere.kopeme", "kopeme-junit3", MavenPomUtil.KOPEME_VERSION, "", null));
      }
      all.add(new RequiredDependency("de.dagere.kopeme", "kopeme-junit5", MavenPomUtil.KOPEME_VERSION, "", null));
      all.add(new RequiredDependency("de.dagere.kopeme", "kopeme-junit4", MavenPomUtil.KOPEME_VERSION, "", null));
      

      all.add(new RequiredDependency("net.kieker-monitoring", "kieker", MavenPomUtil.KIEKER_VERSION, "", null));
      all.add(new RequiredDependency("net.kieker-monitoring", "kieker", MavenPomUtil.KIEKER_VERSION, "", "aspectj"));
      return all;
   }

   public String getGradleDependency() {
      String gradleDependencyString = groupId + ":" + artifactId + ":" + version;
      return gradleDependencyString;
   }

   public Dependency getMavenDependency() {
      final Dependency dependency = new Dependency();
      dependency.setGroupId(groupId);
      dependency.setArtifactId(artifactId);
      dependency.setVersion(version);
      dependency.setScope(scope);
      if (classifier != null) {
         dependency.setClassifier(classifier);
      }
      return dependency;
   }

   public String getGroupId() {
      return groupId;
   }

   public String getArtifactId() {
      return artifactId;
   }

   public String getVersion() {
      return version;
   }

   public String getScope() {
      return scope;
   }

   public String getClassifier() {
      return classifier;
   }
   
}