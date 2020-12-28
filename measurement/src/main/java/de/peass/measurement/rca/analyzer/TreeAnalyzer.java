package de.peass.measurement.rca.analyzer;

import java.util.List;

import de.peass.measurement.rca.data.CallTreeNode;

/**
 * Interface for all classes which determine the nodes that should be run in *one* execution (this excludes LEVELWISE and CONSTANT_LEVELS)
 * 
 * @author reichelt
 *
 */
public interface TreeAnalyzer {
   public List<CallTreeNode> getMeasurementNodesPredecessor();
}
