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

function getAverage(divId, vmValues){
	var averages = [];
 	var select = document.getElementById(divId);
 	var selectedOptions = $('#'+divId+' :selected');
 	for (i = 0; i < selectedOptions.length; i++) {
 	  var vmId = select.options[i].value;
 	  var iteration = vmValues.values[vmId];
 	  var mean = 0;
 	  var count =0;
 	  for (iterationIndex = 0; iterationIndex < iteration.length; iterationIndex++) {
 	    mean+=iteration[iterationIndex].mean*iteration[iterationIndex].n;
 	    count+=iteration[iterationIndex].n;
 	  }
 	  averages[i] = mean/count;
 	}
 	return averages;
}

function visualizeHistogram(){
	var averagesPredecessor = getAverage("predecessorSelect", currentNode.vmValuesPredecessor);
 	var averagesCurrent = getAverage("predecessorSelect", currentNode.vmValues);
 	
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
