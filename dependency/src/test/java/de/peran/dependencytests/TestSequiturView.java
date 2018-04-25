package de.peran.dependencytests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependencyprocessors.ViewNotFoundException;

public class TestSequiturView {

	@Test
	public void testRepetitionRepetition() throws ParseException, IOException, ViewNotFoundException {
		final File clazzDir = new File("target/viewtest/");
		clazzDir.mkdirs();
		ViewGeneratorIT.executeReading(new TestCase("viewtest.TestMe", "test"), clazzDir, new HashMap<>(), "5", new File("src/test/resources/exampletrace/test"));

		final File viewFile = new File(clazzDir, "test_hash_5_method");

		final List<String> expectedCalls = new LinkedList<>();
		expectedCalls.add("viewtest.TestMe#test");
		expectedCalls.add("viewtest.TestMe$InnerClass#<init>([viewtest.TestMe])");
		expectedCalls.add("5x#0(2)");
		expectedCalls.add("viewtest.TestMe$InnerClass#method");
		expectedCalls.add("viewtest.TestMe#staticMethod");
		expectedCalls.add("2x#4(4)");
		expectedCalls.add("viewtest.TestMe#staticMethod");
		expectedCalls.add("5x#0(2)");
		expectedCalls.add("viewtest.TestMe$InnerClass#method");
		expectedCalls.add("viewtest.TestMe#staticMethod");
		expectedCalls.add("viewtest.TestMe#staticMethod");

		try (BufferedReader reader = new BufferedReader(new FileReader(viewFile))) {
			String line;
			while ((line = reader.readLine()) != null) {
				Assert.assertEquals(expectedCalls.remove(0), line.replaceAll(" ", ""));
			}
		}
	}
}
