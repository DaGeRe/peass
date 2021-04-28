package de.dagere.peass;

import java.io.File;
import java.util.List;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.dependency.ClazzFileFinder;

public class TestPackageFinder {
	
	@Test
	public void testDependencyModule(){
		final List<String> lowestPackage = ClazzFileFinder.getClasses(new File("."));
		System.out.println(lowestPackage);
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.dagere.peass.DependencyReadingParallelStarter"));
		Assert.assertThat(lowestPackage, Matchers.not(IsCollectionContaining.hasItem("de.dagere.peass.DependencyReadingParallelStarter.DependencyReadingParallelStarter")));
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.dagere.peass.statistics.DependencyStatisticAnalyzer"));
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.dagere.peass.statistics.DependencyStatistics"));
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.dagere.peass.TestPackageFinder"));
	}
}
