if [ $# -ne 1 ]; then
        echo "Repo-URL muss übergeben werden"
        exit 1
fi

FOLDER=$(echo $1 | awk -F"/" '{print $NF}' | cut -d"." -f1)

echo "Ordner: $FOLDER"

cd /home/diss/evaluation/projekte/ekstazi/ && git clone $1

cp -r /home/diss/evaluation/projekte/ekstazi/$FOLDER /home/diss/evaluation/projekte/empty/
cp -r /home/diss/evaluation/projekte/ekstazi/$FOLDER /home/diss/evaluation/projekte/infinitest/
#cd /home/diss/evaluation/projekte/infinitest/ && git clone $1

P1=$(tmux new-window -n $FOLDER -c "/home/diss/evaluation/performanzanalysator/evaluation/" -dP)
echo $P1
P2=$(tmux split-window -c "/home/diss/evaluation/performanzanalysator/evaluation/" -v -dP -t $P1)

P3=$(tmux split-window -c "/home/diss/evaluation/performanzanalysator/evaluation/" -v -dP -t $P1)
echo $P1
echo "Panels: $P1 $P2 $P3"
#tmux send-keys -t $P1 -l "git clone $1"
#tmux send-keys -t $P2 -l "ls"
#tmux send-keys -t $P2 Enter
tmux send-keys -t $P2 -l "java -cp target/evaluation-0.1-SNAPSHOT.jar de.peran.evaluation.ekstazi.EkstaziEvaluation ../../projekte/ekstazi/$FOLDER | tee evaluation_ekstazi_$FOLDER.txt"
tmux send-keys -t $P2 Enter

tmux send-keys -t $P1 -l "java -cp target/evaluation-0.1-SNAPSHOT.jar de.peran.evaluation.infinitest.InfinitestEvaluation ../../projekte/infinitest/$FOLDER | tee evaluation_infinitest_$FOLDER.txt"
tmux send-keys -t $P1 Enter

P4=$(tmux split-window -c "/home/diss/evaluation/performanzanalysator/evaluation/" -v -dP -t $P1)
tmux send-keys -t $P4 -l "java -cp target/evaluation-0.1-SNAPSHOT.jar de.peran.evaluation.EmptyEvaluation ../../projekte/empty/$FOLDER | tee evaluation_empty_$FOLDER.txt"
tmux send-keys -t $P4 Enter
