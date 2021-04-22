(function(exports) {

    var rank = {
        /*
         * Standart ranking
         *
         * The MIT License, Copyright (c) 2014 Ben Magyar
         */
        standard: function(array, key) {
            // sort the array
            array = array.sort(function(a, b) {
                var x = a[key];
                var y = b[key];
                return ((x < y) ? -1 : ((x > y) ? 1 : 0));
            });
            // assign a naive ranking
            for (var i = 1; i < array.length + 1; i++) {
                array[i - 1]['rank'] = i;
            }
            return array;
        },
        /*
         * Fractional ranking
         *
         * The MIT License, Copyright (c) 2014 Ben Magyar
         */
        fractional: function(array, key) {
            array = this.standard(array, key);
            // now apply fractional
            var pos = 0;
            while (pos < array.length) {
                var sum = 0;
                var i = 0;
                for (i = 0; array[pos + i + 1] && (array[pos + i][key] === array[pos + i + 1][key]); i++) {
                    sum += array[pos + i]['rank'];
                }
                sum += array[pos + i]['rank'];
                var endPos = pos + i + 1;
                for (pos; pos < endPos; pos++) {
                    array[pos]['rank'] = sum / (i + 1);
                }
                pos = endPos;
            }
            return array;
        },
        rank: function(x, y) {
            var nx = x.length,
                ny = y.length,
                combined = [],
                ranked;
            while (nx--) {
                combined.push({
                    set: 'x',
                    val: x[nx]
                });
            }
            while (ny--) {
                combined.push({
                    set: 'y',
                    val: y[ny]
                });
            }
            ranked = this.fractional(combined, 'val');
            return ranked
        }
    };

    /*
    * Error function
    *
    * The MIT License, Copyright (c) 2013 jStat
    */
    var erf = function erf(x) {
        var cof = [-1.3026537197817094, 6.4196979235649026e-1, 1.9476473204185836e-2, -9.561514786808631e-3, -9.46595344482036e-4, 3.66839497852761e-4,
            4.2523324806907e-5, -2.0278578112534e-5, -1.624290004647e-6,
            1.303655835580e-6, 1.5626441722e-8, -8.5238095915e-8,
            6.529054439e-9, 5.059343495e-9, -9.91364156e-10, -2.27365122e-10, 9.6467911e-11, 2.394038e-12, -6.886027e-12, 8.94487e-13, 3.13092e-13, -1.12708e-13, 3.81e-16, 7.106e-15, -1.523e-15, -9.4e-17, 1.21e-16, -2.8e-17
        ];
        var j = cof.length - 1;
        var isneg = false;
        var d = 0;
        var dd = 0;
        var t, ty, tmp, res;

        if (x < 0) {
            x = -x;
            isneg = true;
        }

        t = 2 / (2 + x);
        ty = 4 * t - 2;

        for (; j > 0; j--) {
            tmp = d;
            d = ty * d - dd + cof[j];
            dd = tmp;
        }

        res = t * Math.exp(-x * x + 0.5 * (cof[0] + ty * d) - dd);
        return isneg ? res - 1 : 1 - res;
    };

    /*
    * Normal distribution CDF
    *
    * The MIT License, Copyright (c) 2013 jStat
    */
    var dnorm = function(x, mean, std) {
        return 0.5 * (1 + erf((x - mean) / Math.sqrt(2 * std * std)));
    }

    var statistic = function(x, y) {
        var ranked = rank.rank(x, y),
            nr = ranked.length,
            nx = x.length,
            ny = y.length,
            ranksums = {
                x: 0,
                y: 0
            },
            i = 0, t = 0, nt = 1, tcf, ux, uy;

        while (i < nr) {
            if (i > 0) {
                if (ranked[i].val == ranked[i-1].val) {
                    nt++;
                } else {
                    if (nt > 1) {
                        t += Math.pow(nt, 3) - nt
                        nt = 1;
                    }
                }
            }
            ranksums[ranked[i].set] += ranked[i].rank
            i++;
        }
        tcf = 1 - (t / (Math.pow(nr, 3) - nr))
        ux = nx*ny + (nx*(nx+1)/2) - ranksums.x;
        uy = nx*ny - ux;

        return {
            tcf: tcf,
            ux: ux,
            uy: uy,
            big: Math.max(ux, uy),
            small: Math.min(ux, uy)
        }
    }

    exports.test = function(x, y, alt, corr) {
        // set default value for alternative
        alt = typeof alt !== 'undefined' ? alt : 'two-sided';
        // set default value for continuity
        corr = typeof corr !== 'undefined' ? corr : true;
        var nx = x.length, // x's size
            ny = y.length, // y's size
            f = 1,
            u, mu, std, z, p;

        // test statistic
        u = statistic(x, y);

        // mean compute and correct if given
        if (corr) {
            mu = (nx * ny / 2) + 0.5;
        } else {
            mu = nx * ny / 2;
        }

        // compute standard deviation using tie correction factor
        std = Math.sqrt(u.tcf * nx * ny * (nx + ny + 1) / 12);

        // compute z according to given alternative
        if (alt == 'less') {
            z = (u.ux - mu) / std;
        } else if (alt == 'greater') {
            z = (u.uy - mu) / std;
        } else if (alt == 'two-sided') {
            z = Math.abs((u.big - mu) / std);
        } else {
            console.log('Unknown alternative argument');
        }

        // factor to correct two sided p-value
        if (alt == 'two-sided') {
            f = 2;
        }

        // compute p-value using CDF of standard normal
        p = dnorm(-z, 0, 1) * f;

        return {U: u.small, p: p};
    }

})(typeof exports === 'undefined' ? this['mannwhitneyu'] = {} : exports);

function getVmGraph(currentArray, vmId) {
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
		name: 'VM ' + vmId,
		marker: { size: 3 }
	};
	return data;
}

var xstart = null, xend = null, histStart = null, histEnd = null;

function plotVMGraph(divName, node, ids, idsPredecessor, name) {
	var data = [];
	console.log(ids.length);
	for (idIndex = 0; idIndex < ids.length; idIndex++) {
		var currentId = ids[idIndex];
		data[idIndex] = getVmGraph(node.vmValues.values[currentId], currentId);
	}
	for (idIndex = 0; idIndex < idsPredecessor.length; idIndex++) {
		var currentId = idsPredecessor[idIndex];
		data[idIndex + ids.length] = getVmGraph(node.vmValuesPredecessor.values[currentId], currentId);
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
		var vmId = selectedOptions[i].value;
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
		var selectedOption = selectedOptions[i];
		var vmId = selectedOption.value;
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

function get_t_score(t_array1, t_array2) {
	meanA = jStat.mean(t_array1);
	meanB = jStat.mean(t_array2);
	S2 = (jStat.sum(jStat.pow(jStat.subtract(t_array1, meanA), 2)) + jStat.sum(jStat.pow(jStat.subtract(t_array2, meanB), 2))) / (t_array1.length + t_array2.length - 2);
	t_score = (meanA - meanB) / Math.sqrt(S2 / t_array1.length + S2 / t_array2.length);
	return t_score;
}

function printTTvalue(averagesPredecessor, averagesCurrent) {
	var predecessorStat = new jStat(averagesPredecessor);
	var currentStat = new jStat(averagesCurrent);
	var tscore = get_t_score(averagesPredecessor, averagesCurrent);
	var pval = jStat.ttest(tscore, averagesPredecessor.length + averagesCurrent.length);
	var error = 0.01;
	var x = [2, 4, 6, 2, 3, 7, 5, 1],
            y = [8, 10, 11, 14, 20, 18, 19, 9];
       var mannWhitneyP = mannwhitneyu.test(x, y, alternative = 'two-sided').p;
       console.log(mannWhitneyP);
	document.getElementById("tValueTable").innerHTML = "<h3>Properties without outlier removal</h3>"
		+ "<table><tr><th>Property</th><th>Predecessor</th><th>Current</th></tr>"
		+ "<tr><td>Mean</td><td>" + Math.round(predecessorStat.mean() * 1000) / 1000 + "</td><td>" + Math.round(currentStat.mean() * 1000) / 1000 + "</td></tr>"
		+ "<tr><td>Deviation</td><td>" + Math.round(predecessorStat.stdev() * 1000) / 1000 + "</td><td>" + Math.round(currentStat.stdev() * 1000) / 1000 + "</td></tr>"
		+ "<tr><td>VMs</td><td>" + averagesPredecessor.length +"</td><td>" + averagesCurrent.length + "</td></tr>"
		+ "<tr><td>T-Score</td><td>" + Math.round(tscore*1000)/1000 + "</td></tr>"
		+ "<tr><td>Change T-Test</td><td>" + (pval < error) + "</td></tr>"
		+ "<tr><td>Mann-Whitney p-score</td><td>" + Math.round(mannWhitneyP*1000)/1000 + "</td></tr>"
		+ "<tr><td>Change Mann-Whitney-Test</td><td>" + (mannWhitneyP < error) + "</td></tr>"
		+ "</table>";

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
