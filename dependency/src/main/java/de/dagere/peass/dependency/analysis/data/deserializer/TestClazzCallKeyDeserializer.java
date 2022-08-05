package de.dagere.peass.dependency.analysis.data.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;

public class TestClazzCallKeyDeserializer extends KeyDeserializer {

   @Override
   public Object deserializeKey(final String key, final DeserializationContext ctxt) throws IOException {
      if (key.contains(ChangedEntity.MODULE_SEPARATOR)) {
         String module = key.substring(0, key.indexOf(ChangedEntity.MODULE_SEPARATOR));
         String clazz = key.substring(key.indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, key.length());
         return new TestClazzCall(clazz, module);
      } else {
         return new TestClazzCall(key);
      }

   }

}
