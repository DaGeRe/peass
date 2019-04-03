package de.peass.validation;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.validation.data.ProjectValidation;
import de.peass.validation.data.Validation;
import de.peass.validation.data.ValidationChange;
import de.peran.FolderSearcher;

public class CreateValidationOverview {
	public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
		final File validationFile = new File(args[0]);
		final Validation validation = FolderSearcher.MAPPER.readValue(validationFile, Validation.class);

		int beforeAll = 0;
		int correctAll = 0;
		int sumAll = 0;
		System.out.println("Projekt & Performanzverbesserungen & Gefunden & Nicht Untersucht");
		for (final Map.Entry<String, ProjectValidation> project : validation.getProjects().entrySet()) {
			int before = 0;
			int correct = 0;
			int incorrect = 0;
			int unchanged = 0;
			int unselected = 0;
			for (final ValidationChange change : project.getValue().getChanges().values()) {
				if (change.getType().equals("BEFORE")) {
					before++; beforeAll++;
				} else if (change.getType().equals("MEASURED_CORRECT")) {
					correct++;correctAll++;
				} else if (change.getType().equals("MEASURED_UNCORRECT")) {
					incorrect++;
				} else if (change.getType().equals("NOT_SELECTED")) {
					unselected++;
				} else if (change.getType().equals("MEASURED_UNCHANGED")) {
					unchanged++;
				}
			}
			sumAll+=correct+incorrect+unchanged;
			final double correctPercent = (correct / ((double) correct + incorrect + unchanged));
			System.out.println(project.getKey() + " & " + (correct + incorrect + unchanged) + " & " + correct + "(" + correctPercent + ")" + " & " + before+"\\\\");
		}
		System.out.println("Summe  & " + (sumAll) + " & " + correctAll + "(" + ((double) correctAll / sumAll) + ")" + " & " + beforeAll);

	}
}
