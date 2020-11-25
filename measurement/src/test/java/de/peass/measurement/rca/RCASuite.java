package de.peass.measurement.rca;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.peass.measurement.rca.strategytest.CauseSearcherCompleteTest;
import de.peass.measurement.rca.strategytest.LevelCauseSearcherTest;
import de.peass.measurement.rca.treeanalysis.TestLevelDifferentNodeDeterminer;

@RunWith(Suite.class)
@SuiteClasses({
   LevelCauseSearcherTest.class,
   CauseSearcherCompleteTest.class,
   TestTreeFilter.class,
   TreeUtilTest.class,
   TestLevelDifferentNodeDeterminer.class
})
public class RCASuite {

}
