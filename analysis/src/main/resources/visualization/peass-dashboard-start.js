function getVmGraph(currentArray) {
	var xvals = [], yvals = [];
	var n = 0;
	for (i = 0; i < currentArray.length; i++) {
		xvals[i] = n + currentArray[i].n / 2;
		yvals[i] = currentArray[i].mean;
		n += currentArray[i].n;
	}
	var data = {
		x: xvals,
		y: yvals,
		type: 'scatter',
		mode: 'lines+markers',
		marker: { size: 3 }
	};
	return data;
}

var xstart = null, xend = null, histStart = null, histEnd = null;

function plotVMGraph(divName, node, ids, idsPredecessor, name) {
	var data = [];
	console.log(ids.length);
	for (id = 0; id < ids.length; id++) {
		data[id] = getVmGraph(node.vmValues.values[id]);
	}
	for (id = 0; id < idsPredecessor.length; id++) {
		data[id + ids.length] = getVmGraph(node.vmValuesPredecessor.values[id]);
	}

	var layout = {
		title: { text: "VM-wise Iteration Durations " + name },
		xaxis: { title: { text: "Iteration" } },
		yaxis: {
			title: { text: "Duration / &#x00B5;s" },
			mode: "lines+markers"
		}
	};
	var graphDiv = document.getElementById(divName);
	Plotly.newPlot(graphDiv, data, layout);
	graphDiv.on('plotly_relayout', function (event) {
		console.log(event);
		xstart = event["xaxis.range[0]"];
		xend = event["xaxis.range[1]"];
		visualizeHistogram();
	});
}


function addOptionSelectbox(divName, id, values) {
	var optionString = '<select id="' + id + '" multiple style="height: 100%">';
	for (var key of Object.keys(values)) {
		optionString += '<option selected value="' + key + '">VM ' + key + '</option>';
	}
	optionString += '</select>';

	document.getElementById(divName).innerHTML = optionString;
}

function visualizeGraph(divId, selectId) {
	var ids = [];
	var select = document.getElementById(selectId);
	var selectedOptions = $('#' + selectId + ' :selected');
	for (i = 0; i < selectedOptions.length; i++) {
		var vmId = select.options[i].value;
		ids[i] = vmId;
	}
	if (selectId == "predecessorSelect") {
		plotVMGraph(divId, currentNode, [], ids, "Predecessor");
	} else {
		plotVMGraph(divId, currentNode, ids, [], "Current");
	}
}

function getAverage(divId, vmValues, start, end) {
	var averages = [];
	var select = document.getElementById(divId);
	var selectedOptions = $('#' + divId + ' :selected');
	for (i = 0; i < selectedOptions.length; i++) {
		var vmId = select.options[i].value;
		var iteration = vmValues.values[vmId];
		var mean = 0;
		var count = 0, addedCount = 0;
		for (iterationIndex = 0; iterationIndex < iteration.length; iterationIndex++) {
			count += iteration[iterationIndex].n;
			if (start == null && end == null) {
				mean += iteration[iterationIndex].mean * iteration[iterationIndex].n;
				addedCount += iteration[iterationIndex].n;
			} else if (count > start && count < end) {
				mean += iteration[iterationIndex].mean * iteration[iterationIndex].n;
				addedCount += iteration[iterationIndex].n;
			}
		}
		averages[i] = mean / addedCount;
	}
	return averages;
}

function get_t_test(t_array1, t_array2) {
	meanA = jStat.mean(t_array1);
	meanB = jStat.mean(t_array2);
	S2 = (jStat.sum(jStat.pow(jStat.subtract(t_array1, meanA), 2)) + jStat.sum(jStat.pow(jStat.subtract(t_array2, meanB), 2))) / (t_array1.length + t_array2.length - 2);
	t_score = (meanA - meanB) / Math.sqrt(S2 / t_array1.length + S2 / t_array2.length);
	return t_score;
}

function printTTvalue(averagesPredecessor, averagesCurrent) {
	var predecessorStat = new jStat(averagesPredecessor);
	var currentStat = new jStat(averagesCurrent);
	document.getElementById("tValueTable").innerHTML = "<table><tr><th>Property</th><th>Predecessor</th><th>Current</th></tr>"
		+ "<tr><td>Mean</td><td>" + Math.round(predecessorStat.mean() * 1000) / 1000 + "</td><td>" + Math.round(currentStat.mean() * 1000) / 1000 + "</td></tr>"
		+ "<tr><td>Deviation</td><td>" + Math.round(predecessorStat.stdev() * 1000) / 1000 + "</td><td>" + Math.round(currentStat.stdev() * 1000) / 1000 + "</td></tr>"
		+ "</table>"
		+ "VMS: " + averagesPredecessor.length + " T=" + Math.round(get_t_test(averagesPredecessor, averagesCurrent) * 1000) / 1000;

}

function filterAverages(averages){
	if (histStart == null || histEnd == null){
		return averages;
	}
	var averagesFiltered = [];
	var filteredIndex = 0;
	for (i = 0; i < averages.length; i++){
		if (averages[i] > histStart && averages[i] < histEnd){
			averagesFiltered[filteredIndex++] = averages[i];
		}
	}
	return averagesFiltered;
}

function visualizeHistogram() {
	var averagesPredecessor = getAverage("predecessorSelect", currentNode.vmValuesPredecessor, xstart, xend);
	var averagesCurrent = getAverage("predecessorSelect", currentNode.vmValues, xstart, xend);

	var averagesPredecessorFiltered = filterAverages(averagesPredecessor);
	var averagesCurrentFiltered = filterAverages(averagesCurrent);

	var version = {
		x: averagesCurrentFiltered,
		type: "histogram",
		name: "Version",
		opacity: 0.5,
		marker: {
			color: 'green',
		},
	};
	var predecessor = {
		x: averagesPredecessorFiltered,
		type: "histogram",
		name: "Predecessor",
		opacity: 0.6,
		marker: {
			color: 'red',
		},
	};
	var data = [version, predecessor];
	var layout = {
		barmode: "overlay",
		title: { text: "Histogramm" },
		xaxis: { title: { text: "Duration / &#x00B5;s" } },
		yaxis: { title: { text: "Frequency" } }
	};
	var selectedHistogram = document.getElementById("selectedHistogram");
	Plotly.newPlot(selectedHistogram, data, layout);
	selectedHistogram.on('plotly_relayout', function (event) {
		console.log("Filtering histogram");
		histStart = event["xaxis.range[0]"];
		histEnd = event["xaxis.range[1]"];
		visualizeHistogram();
	});
	printTTvalue(averagesPredecessorFiltered, averagesCurrentFiltered)
}

function visualizeSelected() {
	visualizeHistogram();
	//plotOverallHistogram("selectedHistogram", currentNode);

	visualizeGraph("graphPredecessor", "predecessorSelect");
	visualizeGraph("graphCurrent", "currentSelect");
}

function findGetParameter(parameterName) {
	var result = null,
		tmp = [];
	var items = location.search.substr(1).split("&");
	for (var index = 0; index < items.length; index++) {
		tmp = items[index].split("=");
		if (tmp[0] === parameterName) result = decodeURIComponent(tmp[1]);
	}
	return result;
}

var call = findGetParameter("call");
var ess = findGetParameter("ess");
console.log("Searching " + call + " " + ess);

function findNode(node, depth) {
	if (call == "overall" && ess == -1) {
		return kopemeData[0];
	}
	if (ess == depth) {
		console.log("Testing " + node.call + " " + call);
		if (node.call == call) {
			return node;
		}
	} else {
		for (var index = 0; index < node.children.length; index++) {
			var child = node.children[index];
			var found = findNode(child, depth + 1);
			if (found != null) {
				return found;
			}
		}
	}
}

var currentNode = findNode(treeData[0], 0);
if (currentNode == null) {
	alert("Did not find node with Execution Stack Size " + ess + " and call " + call + " - visualization root node instead");
	currentNode = treeData[0];
}

document.getElementById("overallHistogram").innerHTML = "Current Node: " + currentNode.call

plotOverallHistogram("overallHistogram", currentNode);

addOptionSelectbox('predecessorOptions', 'predecessorSelect', currentNode.vmValuesPredecessor.values);
addOptionSelectbox('currentOptions', 'currentSelect', currentNode.vmValues.values);

visualizeHistogram();
