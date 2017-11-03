package de.peran.measurement.analysis.knowledge;

/*-
 * #%L
 * peran-analysis
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


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

	private final static ObjectMapper objectMapper = new ObjectMapper();

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		VersionKnowledge merged = new VersionKnowledge();
		for (File json : AnalyseOneTest.RESULTFOLDER.listFiles()) {
			if (json.getName().endsWith(".json")) {
				VersionKnowledge knowledge = objectMapper.readValue(json, VersionKnowledge.class);
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

		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(mergedFile, merged);
	}
}
