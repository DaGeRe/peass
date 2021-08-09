package de.peran;

import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Analyzes data from all subfolders of one folder. It is assumed that the typical PeASS-folder-structure is given.
 * 
 * @author reichelt
 *
 */
public class FolderSearcher {
   private static final Logger LOG = LogManager.getLogger(FolderSearcher.class);

   public static final String DATA = "data";

   public static final Option DATAOPTION = Option.builder(DATA).required(true).hasArgs()
         .desc("Data folders that should be analyzed").build();

}
