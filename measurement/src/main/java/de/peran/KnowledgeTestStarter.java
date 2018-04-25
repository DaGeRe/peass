package de.peran;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.peran.analysis.knowledge.VersionKnowledge;
import de.peran.utils.OptionConstants;

public class KnowledgeTestStarter extends DependencyTestPairStarter {

	private final VersionKnowledge knowledge;
	
	public KnowledgeTestStarter(String[] args) throws ParseException, JAXBException, IOException {
		super(args);
		
		final File knowledgeFile = new File(line.getOptionValue(OptionConstants.WARMUP.getName()));
		knowledge = new ObjectMapper().readValue(new File("file.json"), VersionKnowledge.class);
		
	}

	public static void main(final String[] args) throws ParseException, JAXBException, IOException {
		final KnowledgeTestStarter starter = new KnowledgeTestStarter(args);
		starter.processCommandline();
	}

}
