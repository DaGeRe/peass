function getSum {
  awk '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)" "sum/NR" "NR}'
}

function printTValue {
	if [ $# -lt 2 ]; then
		  echo "Two CSV-files for analysis need to be given"
	fi        
	mean1=$(awk -vOFMT=%.10g -F ';' '{print $1}' $1 | awk -vOFMT=%.10g '{sum += $1; square += $1^2} END {print sum / NR}')
    mean2=$(awk -vOFMT=%.10g -F ';' '{print $1}' $2 | awk -vOFMT=%.10g '{sum += $1; square += $1^2} END {print sum / NR}')
    deviation1=$(awk -vOFMT=%.10g -F ';' '{print $1}' $1 | awk -vOFMT=%.10g '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)}')
    deviation2=$(awk -vOFMT=%.10g -F ';' '{print $1}' $2 | awk -vOFMT=%.10g '{sum += $1; square += $1^2} END {print sqrt(square / NR - (sum/NR)^2)}')
	size=$(cat $1 | wc -l)	
	sizefactor=$(echo "sqrt ("$size*$size/"("$size+$size"))" | bc -l)
	weighteddeviation=$(echo "sqrt(("$deviation1*$deviation1"/2)+("$deviation2*$deviation2"/2))" | bc -l)
	tvalue=$(echo "$sizefactor*($mean1-$mean2)/$weighteddeviation" | bc -l)
	#echo "Means: $mean1 $mean2 Deviations: $deviation1 $deviation2 Sizefactor: $sizefactor Weighted: $weighteddeviation"
	echo "T-Value: $tvalue Degrees of Freedom: "$(echo $size*2-2 | bc -l)
}

function writeValues {
	source=$1
	target=$2
	
	if [ -f $target ]
	then
		rm $target
	fi
	
	for file in $(ls $source/ | grep -v "testMe")
	do
		cat $source/$file/testMe/kieker*/*csv | awk -F';' '{print $5}' | tail -n 3 | getSum >> $target
		current=$(cat $source/$file/*/kieker*/*csv | awk -F';' '{sum+=$7} END {print sum}')
		if [ ! $before == $current ]
		then
			echo "Changed value count: $current"
		fi
		before=$current
	done
}

function printValues {
	filePredecessor=$1
	fileCurrent=$2
	echo -n "Predecessor: "
	cat $filePredecessor | getSum
	echo -n "Current: "
	cat $fileCurrent | getSum
	printTValue $filePredecessor $fileCurrent
	
}

files=(level/*/*/*)

if [ ${#files[@]} -gt 1 ]
then
	echo "This script provides an ad-hoc analysis to see the ordner of magnitude of measurement values; there is no outlier detection or warmup consideration, therefore these values are not final!"
	echo "Current file: "${files[0]}/
	cat ${files[0]}/testMe* | grep "<value>" | tr -d "<value/>" | sort > level/current.csv 
	cat ${files[1]}/testMe* | grep "<value>" | tr -d "<value/>" | sort > level/predecessor.csv 
	
	writeValues ${files[0]}/ level/temp1.csv
	cat level/temp1.csv | sort -k 2 | awk '{print $2}' > level/current_vals.csv
	writeValues ${files[1]}/ level/temp2.csv
	cat level/temp2.csv | sort -k 2 | awk '{print $2}' > level/predecessor_vals.csv
	
	echo "Measured KoPeMe"
	printValues level/predecessor.csv level/current.csv
	echo
	
	echo "Measured Kieker"
	printValues level/predecessor_vals.csv level/current_vals.csv
else
	echo "No measurement values yet"
fi
