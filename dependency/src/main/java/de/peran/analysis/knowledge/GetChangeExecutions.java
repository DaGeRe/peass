package de.peran.analysis.knowledge;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Creates java-shell-calls, which can be used to re-measure tests which has been flagged as changes
 * 
 * @author reichelt
 *
 */
public class GetChangeExecutions {
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		File changeData1 = new File(args[0]);

		VersionKnowledge version1 = new ObjectMapper().readValue(changeData1, VersionKnowledge.class);

		int changedVersions = 0, tests = 0;
		for (Entry<String, Changes> version : version1.getVersionChanges().entrySet()) {
			if (version.getValue().getTestcaseChanges().size() > 0) {
				for (Entry<String, List<Change>> testcase : version.getValue().getTestcaseChanges().entrySet()) {
					for (Change method : testcase.getValue()) {
						System.out.println("java -cp target/measurement-0.1-SNAPSHOT.jar de.peran.RemeasureOneTest "
								+ "-endversion " + version.getKey() + " "
								+ "-test " + testcase.getKey() + "#" + method.getClazz() + " "
								+ "-duration 120000 -vms 10 "
								+ "-folder ../../projekte/commons-io");
						tests++;
					}
				}
				// System.out.println(version.getKey());

				changedVersions++;
			}
		}
		System.out.println("Versionen: " + changedVersions + " Tests: " + tests);

	}

}
