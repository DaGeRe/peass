package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependencytests.helper.FakeFileIterator;

public class TestContinuousDependencyReader {

   public static final File CURRENT = new File("target", "current");

   @Test
   public void testDependencReading() throws JsonParseException, JsonMappingException, JAXBException, IOException, InterruptedException, XmlPullParserException {
      FakeFileIterator iterator = new FakeFileIterator(CURRENT, 
            Arrays.asList(new File[] {
                  new File("../dependency/src/test/resources/dependencyIT/basic_state"), 
                  new File("../dependency/src/test/resources/dependencyIT/changed_class")}));
      
      File tempResultFile = new File("target", "dependencies.json");
      
      ContinuousDependencyReader reader = new ContinuousDependencyReader(iterator.getTag(), CURRENT, tempResultFile);
      reader.getDependencies(iterator, "");
      
   }
}
