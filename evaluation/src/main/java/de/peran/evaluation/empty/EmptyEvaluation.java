package de.peran.evaluation.empty;

/*-
 * #%L
 * peran-evaluation
 * %%
 * Copyright (C) 2017 DaGeRe
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
