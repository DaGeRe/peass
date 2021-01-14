package de.peass.measurement.rca.data;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import de.peass.dependency.execution.MeasurementConfiguration;

class CallTreeNodeDeserializer extends JsonDeserializer<CallTreeNode> {

   @Override
   public CallTreeNode deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
      final JsonNode node = p.getCodec().readTree(p);
      final String call = node.get("call").asText();
      final String kiekerPattern = node.get("kiekerPattern").asText();
      final String module = node.get("module") != null ? node.get("module").asText() : null;
      final JsonNode children = node.get("children");
      MeasurementConfiguration nullConfig = null;
      final CallTreeNode root = new CallTreeNode(call, kiekerPattern, null, nullConfig);
      root.setModule(module);
      handleChild(children, root);

      return root;
   }

   private void handleChild(final JsonNode children, final CallTreeNode parent) {
      for (final JsonNode child : children) {
         final String call = child.get("call").asText();
         final String kiekerPattern = child.get("kiekerPattern").asText();
         final CallTreeNode created = parent.appendChild(call, kiekerPattern, null);
         handleChild(child.get("children"), created);
      }
   }
}