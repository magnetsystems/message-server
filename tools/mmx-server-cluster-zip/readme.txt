To Execute
----------
mvn clean package

Main files
----------
pom.xml - contains the logic to assembly the zip file without the final zipping.
bin.xml - contains the logic to zip up the final zip file.

Note:
In pom.xml, I have put in "<!-- step #" to show the order of execution and that is the actual order of
execution. Please note that the actual order of execution is different than the order of <execution> 
listed in the file because mvn executes the execution by the phase that is associated with the plugin.
