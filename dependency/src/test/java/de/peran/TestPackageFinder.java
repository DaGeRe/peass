package de.peran;

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
