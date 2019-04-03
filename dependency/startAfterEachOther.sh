#for project in "commons-io" "commons-fileupload" "commons-dbcp" 
for project in  commons-compress commons-csv commons-dbcp commons-fileupload commons-imaging commons-io commons-jcs commons-numbers commons-pool commons-text
do
	echo "$project"
	git clone https://github.com/apache/$project.git ../../projekte/$project &> clone_$project
	java -Xmx50G -jar target/dependency-0.1-SNAPSHOT.jar -folder ../../projekte/$project/ -threads 30 &> der_$project
	tar -I pxz -cf der_$project.tar.xz der_$project
	rm der_$project
done
