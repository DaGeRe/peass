package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.traces.ViewGenerator;

/**
 * Generates a) Trace-Method-Diff and b) Trace-Method-Source-Diff from a project by loading every version, executing it with instrumentation and afterwards closing it.
 * 
 * @author reichelt
 *
 */
public class ViewPrintStarter {

	public static void main(final String[] args) throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {
		final ViewGenerator tr = new ViewGenerator(args);
		tr.processCommandline();
	}

}
