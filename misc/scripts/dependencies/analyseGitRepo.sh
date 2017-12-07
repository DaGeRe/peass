#!/bin/bash

if [ $# -ne 1 ]; then
        echo "Repo-URL muss übergeben werden"
        exit 1
fi

PEASSHOME=$(pwd)/../../../../

echo $PEASSHOME

FOLDER=$(echo $1 | awk -F"/" '{print $NF}' | cut -d"." -f1)

echo "Ordner: $FOLDER"

cd $PEASSHOME"projekte" && git clone $1

P1=$(tmux new-window -n $FOLDER -c "$PEASSHOME"projekte"/$FOLDER" -dP)
echo $P1
P2=$(tmux split-window -c $PEASSHOME"performanzanalysator/dependency/" -v -dP -t $P1)
echo $P1
#tmux send-keys -t $P1 -l "git clone $1"
tmux send-keys -t $P1 -l "ls"
tmux send-keys -t $P1 Enter
tmux send-keys -t $P2 -l "java -Xmx20g -jar target/dependency-0.1-SNAPSHOT.jar -folder ../../projekte/$FOLDER | tee depanalyse_$FOLDER.txt && java -Xmx20g -cp target/dependency-0.1-SNAPSHOT.jar de.peran.ViewPrintStarter -dependencyfile results/deps_$FOLDER.xml -folder ../../projekte/$FOLDER &> results/viewanalyse_$FOLDER.txt"
# tmux send-keys -t $P2 -l "java -Xmx20g -jar target/dependency-0.1-SNAPSHOT.jar -folder ../../projekte/$FOLDER"
tmux send-keys -t $P2 Enter

