package de.peran.measurement.analysis.knowledge;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.peran.analysis.knowledge.Change;
import de.peran.analysis.knowledge.Changes;
import de.peran.analysis.knowledge.VersionKnowledge;

public class GetChangeClasses {
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		final File f = new File("results/merged.json");
		final VersionKnowledge knowledge = new ObjectMapper().readValue(f, VersionKnowledge.class);
		final Map<String, DescriptiveStatistics> clazzes = new HashMap<>();
		for (final Changes changes : knowledge.getVersionChanges().values()) {
			for (final List<Change> change : changes.getTestcaseChanges().values()) {
				for (final Change c : change) {
					if (c.getCorrectness() != null && c.getCorrectness().equals("CORRECT")) {
						String type = c.getType();
						if (type.equals("FUNCTION") || type.equals("BUGFIX") || type.equals("FEATURE")){
							type = "FUNCTIONALITY";
						}
						if (type.equals("JUNIT") ){
							type = "LIB";
						}
						DescriptiveStatistics clazz = clazzes.get(type);
						if (clazz == null) {
							clazz = new DescriptiveStatistics();
							clazzes.put(type, clazz);
						}
						clazz.addValue(c.getChangePercent());
					}
				}
			}
		}
		for (final Map.Entry<String, DescriptiveStatistics> clazz : clazzes.entrySet()) {
			System.out.println(clazz.getKey() + " " + clazz.getValue().getMean());
		}
	}
}
