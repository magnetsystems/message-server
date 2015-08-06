# change to scripts test directory
#cd tools/mmx-sanity-app/src/main/scripts

# check props file and copy over
if [ -f /mnt/jenkins/slave_files/workspace/MMX_sandbox_heartbeat-prod_J2E/heartbeat-prod.props ]; then
    echo copy props file
    rm -f heartbeat-prod.props
    cp -f /mnt/jenkins/slave_files/workspace/MMX_sandbox_heartbeat-prod_J2E/heartbeat-prod.props .
else
   echo not able to locate heartbeat-prod.props
fi

# check jar file
if [ -f /mnt/jenkins/.m2/repository/com/magnet/mmx/mmx-sanity-app/1.3.6/mmx-sanity-app-1.3.6-shaded.jar ]; then
    echo copying jar file
    rm -f mmx-sanity-app-1.3.6-shaded.jar
    cp -f /mnt/jenkins/.m2/repository/com/magnet/mmx/mmx-sanity-app/1.3.6/mmx-sanity-app-1.3.6-shaded.jar .
else
	echo not able to locate mmx-sanity-app-1.3.6-shaded.jar
fi

# run J2E test
echo running sanity tests
./run-sanity.sh -a admin -p Pr0duction23$ -u "john.doe" -h prod-mmx-001.magnet.com -d prod-mmx-001 heartbeat-prod
rm -f *.bin

# clean up and backup files again
if [ -f heartbeat-prod.props ] && [ -f mmx-sanity-app-1.3.6-shaded.jar ]; then
	echo copying props file back to workspace
	rm -f /mnt/jenkins/slave_files/workspace/MMX_sandbox_heartbeat-prod_J2E/heartbeat-prod.props
	cp -f heartbeat-prod.props /mnt/jenkins/slave_files/workspace/MMX_sandbox_heartbeat-prod_J2E/heartbeat-prod.props

	echo copying jar file back to workspace
	rm -f /mnt/jenkins/.m2/repository/com/magnet/mmx/mmx-sanity-app/1.3.6/mmx-sanity-app-1.3.6-shaded.jar
	cp -f mmx-sanity-app-1.3.6-shaded.jar /mnt/jenkins/.m2/repository/com/magnet/mmx/mmx-sanity-app/1.3.6/mmx-sanity-app-1.3.6-shaded.jar

else
	echo did not locate jar and props in test directory, failed to copy back
fi
