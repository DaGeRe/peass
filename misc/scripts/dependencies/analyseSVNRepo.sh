#!/bin/bash

if [ $# -ne 2 ]; then
	echo "Repo-URL und Ordnername muss übergeben werden"
	exit 1
fi

FOLDER=$2

echo "Ordner: $FOLDER"

cd /home/diss/projekte && svn checkout --force $1 $2
P1=$(tmux new-window -c "/home/diss/projekte/$2" -dP)
echo $P1
P2=$(tmux split-window -c "/home/diss/performanzanalysator/dependency/" -v -dP -t $P1)
echo $P1
#tmux send-keys -t $P1 -l "git clone $1"
tmux send-keys -t $P1 -l "ls"
tmux send-keys -t $P1 Enter

tmux send-keys -t $P2 -l "export BOOT_LIBS=/home/diss/libs/"
tmux send-keys -t $P2 Enter
tmux send-keys -t $P2 -l "java -Xmx20g -jar target/dependency-0.1-SNAPSHOT.jar -folder ../../projekte/$FOLDER"
tmux send-keys -t $P2 Enter
