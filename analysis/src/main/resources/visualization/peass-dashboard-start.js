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
  	mode: 'lines+markers',
  	marker: {size: 3}
  };
  return data;
}

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
			yaxis: { title: { text: "Duration / &#x00B5;s"},
			mode: "lines+markers" }
		  };
  var graphDiv = document.getElementById(divName);
  Plotly.newPlot(graphDiv, data, layout);
  graphDiv.on('plotly_relayout', function(event){
    console.log(event);
    var xstart=event["xaxis.range[0]"];
    var xend=event["xaxis.range[1]"];
    visualizeHistogram(xstart, xend);
    
});
}


function addOptionSelectbox(divName, id, values){
	var optionString='<select id="'+id+'" multiple style="height: 100%">';
	for (var key of Object.keys(values)){
	  optionString+='<option selected value="'+key+'">VM '+key+'</option>';
	}
	optionString+='</select>';

	document.getElementById(divName).innerHTML=optionString;
}

function visualizeGraph(divId, selectId){
	var ids = [];
	var select = document.getElementById(selectId);
 	var selectedOptions = $('#'+selectId+' :selected');
 	for (i = 0; i < selectedOptions.length; i++) {
 	  var vmId = select.options[i].value;
	  ids[i] = vmId;
	}
	if (selectId == "predecessorSelect") {
		plotVMGraph(divId, currentNode, [], ids);
	} else {
		plotVMGraph(divId, currentNode, ids, []);
	}
}

function getAverage(divId, vmValues, start, end){
	var averages = [];
 	var select = document.getElementById(divId);
 	var selectedOptions = $('#'+divId+' :selected');
 	for (i = 0; i < selectedOptions.length; i++) {
 	  var vmId = select.options[i].value;
 	  var iteration = vmValues.values[vmId];
 	  var mean = 0;
 	  var count =0, addedCount = 0;
 	  for (iterationIndex = 0; iterationIndex < iteration.length; iterationIndex++) {
	    count+=iteration[iterationIndex].n;
 	    if (start == null && end == null){
 	      mean+=iteration[iterationIndex].mean*iteration[iterationIndex].n;
 	      addedCount+=iteration[iterationIndex].n;
 	    } else if (count > start && count < end){
 	      mean+=iteration[iterationIndex].mean*iteration[iterationIndex].n;
 	      addedCount+=iteration[iterationIndex].n;
 	    }
 	  }
 	  averages[i] = mean/addedCount;
 	}
 	return averages;
}

function visualizeHistogram(start, end){
	var averagesPredecessor = getAverage("predecessorSelect", currentNode.vmValuesPredecessor, start, end);
 	var averagesCurrent = getAverage("predecessorSelect", currentNode.vmValues, start, end);
 	
 	var version = {
	    x: averagesCurrent,
	    type: "histogram",
	    name: "Version",
	    opacity: 0.5,
	    marker: {
	     color: 'green',
	    },
	  };
	  var predecessor = {
	    x: averagesPredecessor,
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
	  Plotly.newPlot("selectedHistogram", data, layout);

}

function visualizeSelected(){
 	visualizeHistogram();
	//plotOverallHistogram("selectedHistogram", currentNode);
	
	visualizeGraph("graphPredecessor", "predecessorSelect");
	visualizeGraph("graphCurrent", "currentSelect");
}

var currentNode = treeData[0];

plotOverallHistogram("overallHistogram", currentNode);

addOptionSelectbox('predecessorOptions', 'predecessorSelect', currentNode.vmValuesPredecessor.values);
addOptionSelectbox('currentOptions', 'currentSelect', currentNode.vmValues.values);