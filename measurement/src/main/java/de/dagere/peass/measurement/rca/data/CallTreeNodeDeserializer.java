package de.dagere.peass.measurement.rca.data;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import de.dagere.peass.config.MeasurementConfig;

class CallTreeNodeDeserializer extends JsonDeserializer<CallTreeNode> {

   @Override
   public CallTreeNode deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
      final JsonNode node = p.getCodec().readTree(p);
      final String call = node.get("call").asText();
      final String kiekerPattern = !node.get("kiekerPattern").isNull() ? node.get("kiekerPattern").asText() : null;
      JsonNode moduleNode = node.get("module");
      final String module = (moduleNode != null && !moduleNode.isNull()) ? moduleNode.asText() : null;
      final JsonNode children = node.get("children");
      final CallTreeNode root = new CallTreeNode(call, kiekerPattern, null, (MeasurementConfig) null);
      root.setModule(module);
      handleChild(children, root);

      return root;
   }

   private void handleChild(final JsonNode children, final CallTreeNode parent) {
      for (final JsonNode child : children) {
         final String call = child.get("call").asText();
         final String kiekerPattern = child.get("kiekerPattern").asText();
         JsonNode moduleNode = child.get("module");
         final String module = (moduleNode != null && !moduleNode.isNull()) ? moduleNode.asText() : null;
         final CallTreeNode created = parent.appendChild(call, kiekerPattern, null);
         created.setModule(module);
         handleChild(child.get("children"), created);
      }
   }
}