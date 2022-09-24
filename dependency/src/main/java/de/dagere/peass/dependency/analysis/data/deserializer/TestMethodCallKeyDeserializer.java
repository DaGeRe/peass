package de.dagere.peass.dependency.analysis.data.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class TestMethodCallKeyDeserializer extends KeyDeserializer {

   @Override
   public Object deserializeKey(final String key, final DeserializationContext ctxt) throws IOException {
      return TestMethodCall.createFromString(key);
   }

}
