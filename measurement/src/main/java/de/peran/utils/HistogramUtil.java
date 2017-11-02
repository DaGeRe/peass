package de.peran.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

/**
 * Creates Historgrames out of performance measurements.
 * 
 * @author reichelt
 *
 */
public final class HistogramUtil {
	
	private HistogramUtil(){
		
	}

	private static final Logger LOG = LogManager.getLogger(HistogramUtil.class);

	public static void createHistogram(final File chartfile, final double[][] allDoubleValues) {
		for (int i = 0; i < allDoubleValues.length; i++) {
			if (allDoubleValues[i].length == 0) {
				throw new RuntimeException("Leeres Array übergeben: " + i);
			}
		}
		final HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.RELATIVE_FREQUENCY);
		LOG.trace("Anzahl: " + allDoubleValues.length);

		for (int i = 0; i < allDoubleValues.length; i++) {
			LOG.debug("Werte: {}", allDoubleValues[i].length);
			final double min = Arrays.stream(allDoubleValues[i]).min().getAsDouble();
			final double max = Arrays.stream(allDoubleValues[i]).max().getAsDouble();
			final double avg = Arrays.stream(allDoubleValues[i]).average().getAsDouble();
			final List<Double> filtered = new LinkedList<>();
			for (final double value : allDoubleValues[i]) {
				if (value < avg * 3) {
					filtered.add(value);
				}
			} 
			final double[] filteredArray = ArrayUtils.toPrimitive(filtered.toArray(new Double[0]));
			LOG.debug("Werte: {} - {}", min, max);
			LOG.debug("Vals: {} {}", i, Arrays.toString(filteredArray));
			dataset.addSeries("Darstellung", filteredArray, 50);
		}
		final String plotTitle = "Histogram";
		final String xaxis = "Ausführungsdauer";
		final String yaxis = "Häufigkeit";
		final PlotOrientation orientation = PlotOrientation.VERTICAL;
		final boolean show = true;
		final boolean toolTips = false;
		final boolean urls = false;
		final JFreeChart chart = ChartFactory.createHistogram(plotTitle, xaxis, yaxis, dataset, orientation, show, toolTips, urls);
		final int width = 1000;
		final int height = 500;

		try {
			ChartUtilities.saveChartAsPNG(chartfile, chart, width, height);
			LOG.debug("Datei erstellt: " + chartfile.getAbsolutePath());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
