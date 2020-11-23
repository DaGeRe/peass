file=$(echo rca/level/*/*/*/*/*/kieker*/ | awk '{print $1}' )
cat $file/measurement* | awk -F';' '{print $1}' | sort | uniq |
        while read method
        do
                echo -n "$method "
                cat $file/measurement* | grep "$method" | awk -F';' '{sum+=$7} END {print sum}'
        done
