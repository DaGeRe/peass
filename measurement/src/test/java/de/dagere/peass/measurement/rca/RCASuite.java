package de.dagere.peass.measurement.rca;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.dagere.peass.measurement.rca.strategytest.CauseSearcherCompleteTest;
import de.dagere.peass.measurement.rca.strategytest.LevelCauseSearcherTest;
import de.dagere.peass.measurement.rca.treeanalysis.TestLevelDifferentNodeDeterminer;

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
