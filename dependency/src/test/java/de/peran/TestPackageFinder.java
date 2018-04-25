package de.peran;

import java.io.File;
import java.util.List;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;

import de.peran.dependency.ClazzFinder;

public class TestPackageFinder {
	
	@Test
	public void testDependencyModule(){
		final List<String> lowestPackage = ClazzFinder.getClasses(new File("."));
		System.out.println(lowestPackage);
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.peran.DependencyReadingStarter"));
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.peran.statistics.DependencyStatisticAnalyzer"));
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.peran.statistics.DependencyStatistics"));
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.peran.TestPackageFinder"));
	}
}
