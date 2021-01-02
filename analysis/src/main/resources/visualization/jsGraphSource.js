
function plotOverallHistogram(divName, node){
  var version = {
    x: node.values,
    type: "histogram",
    name: "Version",
    opacity: 0.5,
    marker: {
     color: 'green',
    },
  };
  var predecessor = {
    x: node.valuesPredecessor,
    type: "histogram",
    name: "Predecessor",
    opacity: 0.6,
    marker: {
     color: 'red',
    },
  };
  var data = [version, predecessor];
  var layout = {barmode: "overlay", 
			title: { text: "Histogramm"},
			xaxis: { title: { text: "Duration / &#x00B5;s"} },
			yaxis: { title: { text: "Frequency"} }
		  };
  Plotly.newPlot(divName, data, layout);
  
  currentNode = node;
}
