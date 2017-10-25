package de.peran;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import de.peran.dependency.PackageFinder;

public class TestPackageFinder {
	
	@Test
	public void testDependencyModule(){
		String lowestPackage = PackageFinder.getLowestPackageOverall(new File("."));
		Assert.assertEquals("de.peran", lowestPackage);
	}
}
