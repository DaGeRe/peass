package de.peran.measurement.analysis.knowledge;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peran.AnalyseOneTest;
import de.peran.analysis.knowledge.Change;
import de.peran.analysis.knowledge.Changes;
import de.peran.analysis.knowledge.VersionKnowledge;

public class MergeChangeData {

	private final static ObjectMapper MAPPER = new ObjectMapper();

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		VersionKnowledge merged = new VersionKnowledge();
		for (File json : AnalyseOneTest.RESULTFOLDER.listFiles()) {
			if (json.getName().endsWith(".json")) {
				VersionKnowledge knowledge = MAPPER.readValue(json, VersionKnowledge.class);
				for (Map.Entry<String, Changes> entry : knowledge.getVersionChanges().entrySet()) {
					if (!merged.getVersionChanges().containsKey(entry.getKey())) {
						merged.getVersionChanges().put(entry.getKey(), entry.getValue());
					} else {
						Changes current = entry.getValue();
						Changes mergedVersion = merged.getVersion(entry.getKey());
						for (Map.Entry<String, List<Change>> changes : current.getTestcaseChanges().entrySet()) {
							if (mergedVersion.getTestcaseChanges().containsKey(changes.getKey())) {
								List<Change> mergedChanges = mergedVersion.getTestcaseChanges().get(changes.getKey());
								for (Change change : changes.getValue()) {
									boolean found = false;
									for (Change mergedChange : mergedChanges) {
										if (mergedChange.getDiff().equals(change.getDiff())) {
											found = true;
											if (mergedChange.getCorrectness() == null && change.getCorrectness() != null) {
												mergedChange.setCorrectness(change.getCorrectness());
												mergedChange.setType(change.getType());
											}
										}
									}
									if (!found) {
										mergedChanges.add(change);
									}
								}
							} else {
								mergedVersion.getTestcaseChanges().put(changes.getKey(), changes.getValue());
							}
						}
					}
				}
			}
		}
		
		merged.getVersionChanges().forEach((version, changes) -> {
			changes.getTestcaseChanges().forEach((test, changeList) -> {
				changeList.forEach(change -> {
					if (change.getCorrectness() != null && change.getCorrectness().equals("Y")){
						change.setCorrectness("CORRECT");
					}
				});
			});
		});

		File mergedFile = new File(AnalyseOneTest.RESULTFOLDER, "merged.json");

		MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
		MAPPER.writeValue(mergedFile, merged);
	}
}
