function getMin(node){
  var minValue = Number.MAX_VALUE;
  for (const value of node.valuesPredecessor) {
	minValue = Math.min(minValue, value);
  }
  for (const value of node.values) {
	minValue = Math.min(minValue, value);
  }
  
  return minValue;
}


function plotOverallHistogram(divName, node){
  var predecessorValues = new Array();
  var currentValues = new Array();
  if (node.valuesPredecessor != null && node.values != null) {
    var minValue = getMin(node);
  
    var factor, unit;
    if (minValue <= 1000) {
      unit = "n";
      factor = 1;
    } else if (minValue <= 1000000) {
      unit = "&#x00B5;";
      factor = 1000;
    } else if (minValue <= 1000000000) {
      unit = "m";
      factor = 1000000;
    } else {
      unit = ""
      factor = 1000000000;
    }
  
    for (var i=0; i<node.valuesPredecessor.length; i++) {
  	predecessorValues[i] = node.valuesPredecessor[i] / factor;
    }
  
    for (var i=0; i<node.values.length; i++) {
  	currentValues[i] = node.values[i] / factor;
    }
  }

  var version = {
    x: currentValues,
    type: "histogram",
    name: "Current Commit",
    opacity: 0.5,
    marker: {
     color: 'green',
    },
  };
  var predecessor = {
    x: predecessorValues,
    type: "histogram",
    name: "Predecessor Commit",
    opacity: 0.6,
    marker: {
     color: 'red',
    },
  };
  var data = [version, predecessor];
  var layout = {barmode: "overlay", 
			title: { text: "Histogramm"},
    			xaxis: { title: { text: "Duration / " + unit + "s" } },
			yaxis: { title: { text: "Frequency"} },
			height: 400
		  };
  Plotly.newPlot(divName, data, layout);
  
  currentNode = node;
}
