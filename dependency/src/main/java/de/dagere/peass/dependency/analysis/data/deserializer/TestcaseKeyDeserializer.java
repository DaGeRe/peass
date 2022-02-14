package de.dagere.peass.dependency.analysis.data.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestcaseKeyDeserializer extends KeyDeserializer {

   @Override
   public Object deserializeKey(final String key, final DeserializationContext ctxt) throws IOException {
      return new TestCase(key);
   }

}
