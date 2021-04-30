# This is an automatically generated script to help debugging Peass

echo -n "Current level completed VMs: "
ls rca/level/*/*/*/*.xml | wc -l

echo -n "Completed Levels: "
var=$(ls rca/archived/*/*/*/ | head -n 1); ls rca/archived/*/*/*/$var | wc -l