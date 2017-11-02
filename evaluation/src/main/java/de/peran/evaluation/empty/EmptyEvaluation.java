package de.peran.evaluation.empty;

import java.io.File;
import java.io.IOException;

import de.peran.evaluation.base.EvaluationVersion;
import de.peran.evaluation.base.Evaluator;

/**
 * Determines how much tests are executed without test selection.
 * 
 * @author reichelt
 *
 */
public class EmptyEvaluation extends Evaluator {

	public static void main(final String[] args) {
		final File projectFolder = new File(args[0]);
		final Evaluator evaluator = new EmptyEvaluation(projectFolder);
		evaluator.evaluate();
	}

	public EmptyEvaluation(final File projectFolder) {
		super(projectFolder, "empty");
	}

	@Override
	public void evaluate() {
		final File resultFile = new File(resultFolder, "evaluation_" + projectFolder.getName() + "_empty.json");
		int i = 0;
		while (iterator.hasNextCommit()) {
			iterator.goToNextCommit();

			final File currentFile = new File(debugFolder, "myResult" + i + "_" + iterator.getTag() + ".txt");
			executor.executeTests(currentFile);

			final EvaluationVersion currentVersion = getTestsFromFile(currentFile);
			if (currentVersion.getTestcaseExecutions().size() > 0) {
				evaluation.getVersions().put(iterator.getTag(), currentVersion);
				try {
					OBJECTMAPPER.writeValue(resultFile, evaluation);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			i++;
		}

	}
}
