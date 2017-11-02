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
		List<String> lowestPackage = ClazzFinder.getLowestPackageOverall(new File("."));
		System.out.println(lowestPackage);
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.peran.DependencyReadingStarter"));
		Assert.assertThat(lowestPackage, IsCollectionContaining.hasItem("de.peran.TestPackageFinder"));
	}
}
