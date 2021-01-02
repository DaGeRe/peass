
function getVmGraph(currentArray) {
  var xvals = [], yvals = [];  
  var n = 0;
  for (i = 0; i < currentArray.length; i++){
  	xvals[i] = n + currentArray[i].n/2;
  	yvals[i] = currentArray[i].mean;
  	n+=currentArray[i].n;
  }
  var data = {
  	x: xvals,
  	y: yvals,
  	type: 'scatter',
  	label: "test"
  };
  return data;
}

var currentNode;

function plotVMGraph(divName, node, ids, idsPredecessor){
  var data = [];
  console.log(ids.length);
  for (id = 0; id < ids.length; id++){
  	data[id] = getVmGraph(node.vmValues.values[id]);
  }
  for (id = 0; id < idsPredecessor.length; id++){
  	data[id + ids.length] = getVmGraph(node.vmValuesPredecessor.values[id]);
  }
  
  var layout = {title: { text: "VM-wise Iteration Durations"},
			xaxis: { title: { text: "Iteration"} },
			yaxis: { title: { text: "Duration / &#x00B5;s"} }
		  };
  Plotly.newPlot(divName, data, layout);
}

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
