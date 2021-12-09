package de.dagere.peass.execution.maven.pom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.execution.ProjectModules;

public class ModuleReader {
   private final ProjectModules modules = new ProjectModules(new LinkedList<>());

   public ProjectModules readModuleFiles(final File pom) throws FileNotFoundException, IOException, XmlPullParserException {
      final Model model;
      try (FileInputStream inputStream = new FileInputStream(pom)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         model = reader.read(inputStream);
      }
      if (model.getModules() != null && model.getModules().size() > 0) {
         for (final String module : model.getModules()) {
            final File moduleFolder = new File(pom.getParentFile(), module);
            final File modulePom = new File(moduleFolder, "pom.xml");
            readModuleFiles(modulePom);
            if (!modules.getModules().contains(moduleFolder)) {
               modules.getModules().add(moduleFolder);
            }
         }
      } else {
         modules.getModules().add(pom.getParentFile());
         modules.getArtifactIds().put(pom.getParentFile(), model.getArtifactId());
      }
      return modules;

   }
}
