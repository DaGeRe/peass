package de.dagere.peass.dependency.execution;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Dependency;

class RequiredDependency{
   
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



   public static List<RequiredDependency> getAll(final boolean isJUnit3){
      List<RequiredDependency> all = new LinkedList<RequiredDependency>();
      
      if (isJUnit3) {
         all.add(new RequiredDependency("de.dagere.kopeme", "kopeme-junit3", MavenPomUtil.KOPEME_VERSION, "", null));
      }
      all.add(new RequiredDependency("de.dagere.kopeme", "kopeme-junit", MavenPomUtil.KOPEME_VERSION, "", null));
      
      all.add(new RequiredDependency("net.kieker-monitoring", "kieker", MavenPomUtil.KIEKER_VERSION, "", "jar"));
      all.add(new RequiredDependency("net.kieker-monitoring", "kieker", MavenPomUtil.KIEKER_VERSION, "", "aspectj"));
      
      all.add(new RequiredDependency("org.apache.logging.log4j", "log4j-slf4j-impl", "2.14.0", "", null));
      
      all.add(new RequiredDependency("org.jctools", "jctools-core", "3.3.0", "", null));
      
      all.add(new RequiredDependency("org.aspectj", "aspectjweaver", "1.9.6", "", null));
      
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
   
}