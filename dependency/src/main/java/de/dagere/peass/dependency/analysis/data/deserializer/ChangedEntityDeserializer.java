package de.dagere.peass.dependency.analysis.data.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class ChangedEntityDeserializer extends KeyDeserializer {

   public ChangedEntityDeserializer() {
      }

   @Override
   public ChangedEntity deserializeKey(final String key, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
      String value = key;
      final ChangedEntity entity;

      String method = null;
      if (value.contains(ChangedEntity.METHOD_SEPARATOR)) {
         method = value.substring(value.indexOf(ChangedEntity.METHOD_SEPARATOR) + 1);
         value = value.substring(0, value.indexOf(ChangedEntity.METHOD_SEPARATOR));
      }

      if (value.contains(ChangedEntity.MODULE_SEPARATOR)) {
         final String clazz = value.substring(value.indexOf(ChangedEntity.MODULE_SEPARATOR) + 1);
         final String module = value.substring(0, value.indexOf(ChangedEntity.MODULE_SEPARATOR));
         entity = new ChangedEntity(clazz, module, method);
      } else {
         entity = new ChangedEntity(value, "", method);
      }

      return entity;
   }
}
