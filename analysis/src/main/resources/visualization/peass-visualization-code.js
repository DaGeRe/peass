	
var i = 0,
	duration = 750,
	root;

var tree = d3.layout.tree()
	.size([height, width]);

var diagonal = d3.svg.diagonal()
	.projection(function(d) { return [d.y, d.x]; });

var svg = d3.select("#tree").append("svg")
	.attr("width", width + margin.right + margin.left)
	.attr("height", height + margin.top + margin.bottom)
  .append("g")
	.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

var faster_changed = textures.lines()
.background("#00FF00")
.thicker();

var slower_changed = textures.lines()
.background("#FF0000")
.thicker();

var unknown_changed = textures.lines()
.background("#8888FF")
.thicker();

var equal_changed = textures.lines()
.background("#FFF")
.thicker();

svg.call(faster_changed);
svg.call(slower_changed);
svg.call(unknown_changed);
svg.call(equal_changed);

root = treeData[0];
root.x0 = height / 2;
root.y0 = 0;
  
update(root);

d3.select(self.frameElement).style("height", "500px");

function getTexture(node){
  if (node.hasSourceChange)
  { 
    switch (node.state){
	  case 'FASTER': return faster_changed.url();
	  case 'SLOWER': return slower_changed.url();
	  case 'UNKNOWN': return unknown_changed.url();
	  case null: return equal_changed.url();
	}
  }
  else 
	return node.color;
}

function update(source) {
	
  var t = textures.lines()
	  .thicker();

  // Compute the new tree layout.
  var nodes = tree.nodes(root).reverse(),
	  links = tree.links(nodes);

  // Normalize for fixed-depth.
  nodes.forEach(function(d) { d.y = d.depth * 180; });

  // Update the nodes…
  var node = svg.selectAll("g.node")
	  .data(nodes, function(d) { return d.id || (d.id = ++i); });

  // Enter any new nodes at the parent's previous position.
  var nodeEnter = node.enter().append("g")
	  .attr("class", "node")
	  .attr("transform", function(d) { return "translate(" + source.y0 + "," + source.x0 + ")"; })
	  .on("click", click)
	  .on("mouseover", shownode);

  nodeEnter.append("circle")
	  .attr("r", 1e-6)
	  .style("fill", function(d) { return getTexture(d); });

  nodeEnter.append("text")
	  .attr("x", function(d) { return d.children || d._children ? -13 : 13; })
	  .attr("dy", ".35em")
	  .attr("text-anchor", function(d) { return d.children || d._children ? "end" : "start"; })
	  .text(function(d) { return d.name; })
	  .style("fill-opacity", 1e-6);

  // Transition nodes to their new position.
  var nodeUpdate = node.transition()
	  .duration(duration)
	  .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; });

  nodeUpdate.select("circle")
	  .attr("r", 10)
	  .style("fill", function(d) {  return getTexture(d); });

  nodeUpdate.select("text")
	  .style("fill-opacity", 1);

  // Transition exiting nodes to the parent's new position.
  var nodeExit = node.exit().transition()
	  .duration(duration)
	  .attr("transform", function(d) { return "translate(" + source.y + "," + source.x + ")"; })
	  .remove();

  nodeExit.select("circle")
	  .attr("r", 1e-6);

  nodeExit.select("text")
	  .style("fill-opacity", 1e-6);

  // Update the links…
  var link = svg.selectAll("path.link")
	  .data(links, function(d) { return d.target.id; });

  // Enter any new links at the parent's previous position.
  link.enter().insert("path", "g")
	  .attr("class", "link")
	  .attr("d", function(d) {
		var o = {x: source.x0, y: source.y0};
		return diagonal({source: o, target: o});
	  });

  // Transition links to their new position.
  link.transition()
	  .duration(duration)
	  .attr("d", diagonal);

  // Transition exiting nodes to the parent's new position.
  link.exit().transition()
	  .duration(duration)
	  .attr("d", function(d) {
		var o = {x: source.x, y: source.y};
		return diagonal({source: o, target: o});
	  })
	  .remove();

  // Stash the old positions for transition.
  nodes.forEach(function(d) {
	d.x0 = d.x;
	d.y0 = d.y;
  });
}

// Toggle children on click.
function click(d) {
  if (d.children) {
	d._children = d.children;
	d.children = null;
  } else {
	d.children = d._children;
	d._children = null;
  }
  update(d);
}

function round(value){
	return Math.round(value*1000)/1000;
}

function collapse(){
   root = treeData[0];
   collapseNode(root);
}

function collapseNode(parent){
   for (var nodeId in parent.children) {
      var node=parent.children[nodeId];
      if (node.color == "#5555FF"){
          click(node);
      } else {
	      collapseNode(node);
      }
   }
}

function fallbackCopyTextToClipboard(text) {
  var textArea = document.createElement("textarea");
  textArea.value = text;
  
  // Avoid scrolling to bottom
  textArea.style.top = "0";
  textArea.style.left = "0";
  textArea.style.position = "fixed";

  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();

  try {
    var successful = document.execCommand('copy');
    var msg = successful ? 'successful' : 'unsuccessful';
    console.log('Fallback: Copying text command was ' + msg);
  } catch (err) {
    console.error('Fallback: Oops, unable to copy', err);
  }

  document.body.removeChild(textArea);
}

function diffUsingJS(text1, text2, outputDiv) {
	"use strict";
	var base = difflib.stringAsLines(text1),
		newtxt = difflib.stringAsLines(text2),
		sm = new difflib.SequenceMatcher(base, newtxt),
		opcodes = sm.get_opcodes();

	outputDiv.innerHTML = "";

	outputDiv.appendChild(diffview.buildView({
		baseTextLines: base,
		newTextLines: newtxt,
		opcodes: opcodes,
		baseTextName: "Alte Version",
		newTextName: "Neue Version",
		viewType: 0,
		bothContainLineNumbers: false
	}));
}

function shownode(node) {
  if (node.statistic != null){
	  infos.innerHTML="<table>" +
      "<tr><th>Property</th><th>Predecessor</th><th>Current</th></tr>"+
      "<tr><td>Mean</td><td>" + round(node.statistic.meanOld) +    " &micro;s</td><td>" + round(node.statistic.meanCurrent)+" &micro;s</td></tr>"+
      "<tr><td>Deviation</td><td>" + round(node.statistic.deviationOld)+"</td><td>" + round(node.statistic.deviationCurrent)+"</td></tr>"+
      "<tr><td>In-VM-Deviation</td><td>" + round(node.inVMDeviationPredecessor) + "</td><td>" + round(node.inVMDeviation)+ "</td></tr>" + 
      "</table> VMs: " + node.statistic.vms +
      " T=" + round(node.statistic.tvalue);
  } else {
	  infos.innerHTML = "No statistic";
  }
  if (node.key != node.otherKey){
    diffUsingJS(source["old"][node.key], source["current"][node.otherKey], quelltext);
  } else {
    var sourceCurrent = source["current"][node.key];
    var sourceOld = source["old"][node.key];
    if (sourceCurrent != null && sourceOld != null){
      if (sourceCurrent == sourceOld) {
      	const highlightedCode = hljs.highlight("java", source["current"][node.key]).value;
    	quelltext.innerHTML="<pre>"+highlightedCode+"</pre>";
      } else {
    	  diffUsingJS(sourceOld, sourceCurrent, quelltext);
      }
    }
  }
  
  
  var inspectLink = "";
  if (node.ess != -1){
    inspectLink = "<a href='"+treeData[0].call.replace("#", "_") +"_dashboard.html?ess="+node.ess+"&call=" + encodeURIComponent(node.call)+"' target='parent'>Inspect Node</a><br><br>";
  }
  if (node.kiekerPattern != node.otherKiekerPattern) {
  	histogramm.innerHTML=node.kiekerPattern + " " + node.otherKiekerPattern + inspectLink;
  } else {
  	histogramm.innerHTML=node.kiekerPattern + node.otherKiekerPattern + inspectLink;
  }
  plotOverallHistogram("histogramm", node);
  //document.getElementById("histogramm").innerHtml+="<br><a href='"+treeData[0].call+"_dashboard.html?ess="+1+"&test="+node.call+"'>Inspect Node</a>";
}

shownode(root);
