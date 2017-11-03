package de.peran.dependency.traces;

/*-
 * #%L
 * peran-dependency
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.dependency.analysis.data.TraceElement;

/**
 * Reads the traces of kieker results and combines them with source information read by javaparser.
 * @author reichelt
 *
 */
public class TraceMethodReader {

	private static final Logger LOG = LogManager.getLogger(TraceMethodReader.class);

	private boolean different;
	private int repetitionCount = 0, currentCycleLength = 0;
	private final TraceWithMethods trace = new TraceWithMethods();
	final List<TraceElement> calls;

	private final int LOOKBACK_LENGTH = 15;

	final Map<File, CompilationUnit> loadedUnits = new HashMap<>();
	
	public TraceMethodReader(final List<TraceElement> calls){
		this.calls = calls;
	}
	
	public TraceMethodReader(File traceFolder){
		this.calls = new CalledMethodLoader(traceFolder).getShortTrace(null);
	}

	public TraceWithMethods getTraceWithMethods(final File... clazzFolder) throws ParseException, IOException {
		LOG.info("Trace-Länge: " + calls.size());
		for (int i = 0; i < calls.size(); i++) {
			final TraceElement currentTraceElement = calls.get(i);
			if (i % 10000 == 0) {
				LOG.debug("I: {} Klasse: {} Methode: {} Reps: {} Cycle: {}", i, currentTraceElement.getClazz(), currentTraceElement.getMethod(), repetitionCount, currentCycleLength);
			}

			different = false;
			analyzePotentialCycle(currentTraceElement);
			if (different) {
				LOG.debug("Different");
				writeNextElement(currentTraceElement, clazzFolder);
			}
		}
		writeRepetitionInfo();

		return trace;
	}

	private void writeNextElement(final TraceElement currentTraceElement, final File... clazzFolder) throws ParseException, IOException {
		final File clazzFile = TraceReadUtils.getClazzFile(currentTraceElement, clazzFolder);
		if (clazzFile != null) {
			writeRepetitionInfo();
			repetitionCount = 0;
			currentCycleLength = 0;
			LOG.info("Methode: " + currentTraceElement.getMethod() + " Länge: " + calls.size());
			LOG.info("Suche: {} {} Länge: {}", currentTraceElement.getClazz(), clazzFile, trace.getLength());

			CompilationUnit cu = loadedUnits.get(clazzFile);
			if (cu == null) {
				LOG.info("CU " + clazzFile + " not imported yet");
				cu = JavaParser.parse(clazzFile);
				loadedUnits.put(clazzFile, cu);
			}
			final Node method = TraceReadUtils.getMethod(currentTraceElement, cu);
			trace.addElement(currentTraceElement, method != null ? method.toString().intern() : null);
		} else {
			LOG.error("Klasse nicht gefunden: {} {}", clazzFile, currentTraceElement.getClazz());
		}
	}

	private void analyzePotentialCycle(final TraceElement currentTraceElement) {
		if (currentCycleLength > 0) {
			checkCycleContinuation(currentTraceElement);
		} else {
			checkCycleInitialy(currentTraceElement);
		}
	}

	private void checkCycleInitialy(final TraceElement currentTraceElement) {
		final int lastIndex = trace.getLength() - 1;
		int cycleLength = -1;
		for (int lookback = 0; lookback < Math.min(LOOKBACK_LENGTH, (lastIndex + 1) / 2); lookback++) {
			final TraceElement samePredecessorCandidate = trace.getTraceElement(lastIndex - lookback);
			LOG.debug("Index: " + (lastIndex - lookback) + " Kandidat: " + samePredecessorCandidate.getClazz() + " " + samePredecessorCandidate.getMethod());
			if (TraceReadUtils.traceElementsEquals(currentTraceElement, samePredecessorCandidate)) {
				cycleLength = lookback + 1;
				break;
			}
		}
		LOG.debug("Cyclelength: {} LastIndex: {} ", cycleLength, lastIndex);
		if (cycleLength > 0) {
			trace.addElement(currentTraceElement, null);
			different = checkCycleElements(lastIndex + 1, cycleLength);
			if (!different) {
				currentCycleLength = cycleLength;
				repetitionCount = 2 * currentCycleLength;
				for (int removeIndex = 0; removeIndex < currentCycleLength; removeIndex++) {
					trace.removeElement(trace.getLength() - 1);
				}
			} else {
				trace.removeElement(lastIndex + 1);
			}
		} else {
			different = true;
		}
	}

	private void checkCycleContinuation(final TraceElement currentTraceElement) {
		final int indexInCycle = (currentCycleLength - 1) - (repetitionCount % currentCycleLength);
		final int lookbackPoint = trace.getLength() - 1 - indexInCycle;
		final TraceElement samePredecessorCandidate = trace.getTraceElement(lookbackPoint);
		if (!TraceReadUtils.traceElementsEquals(currentTraceElement, samePredecessorCandidate)) {
			different = true;
		} else {
			repetitionCount++;
		}
	}

	private void writeRepetitionInfo() {
		if (repetitionCount > 0) {
			final int firstDepth = trace.getTraceElement(trace.getLength() - currentCycleLength).getDepth();
			final String methodString = ((repetitionCount / currentCycleLength)) + " repetitions of " + currentCycleLength + " calls";
			trace.addElement(new TraceElement("before", methodString, firstDepth), methodString);
		}
	}

	/**
	 * Checks all elements of a possible cycle, whether they realy are a cycle.
	 * 
	 * @param trace
	 *            Trace that should be checked
	 * @param lastIndex
	 *            Index, where the cycle begins
	 * @param cycleLengh
	 *            Length of the cycle
	 * @return
	 */
	private boolean checkCycleElements(final int lastIndex, final int cycleLengh) {
		for (int lookback = 1; lookback < cycleLengh; lookback++) {
			final TraceElement firstCycleElement = trace.getTraceElement(lastIndex - lookback);
			final TraceElement secondCycleElement = trace.getTraceElement(lastIndex - cycleLengh - lookback);
			if (!TraceReadUtils.traceElementsEquals(firstCycleElement, secondCycleElement)) {
				LOG.debug("Kein Zyklus, ungleich: " + firstCycleElement.getMethod() + " " + secondCycleElement.getMethod());
				return true;
			}
			// TODO Prüfen, ob alle Elemente der Kette gleich sind; wenn ja: kein TraceElement hinzufügen
		}
		return false;
	}

}
