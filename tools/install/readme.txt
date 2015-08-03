installMMXCloud.sh is for installing Openfire on a brand-new ec2 instance. User interaction required.

updateMMXCloud.sh is for updating the plugin on a pre-openfire-installed instance. It will:
- refresh the plugin
- optionally refresh the db
- optionally seed test data

Assumption
----------
1. Refreshing the plugin jar and files will refresh the plugin for openfire.

Instructions for running from a developer machine
-------------------------------------------------
1. On a developer’s mac, update setEnv.sh with the required fields. E.g., the instance name/ip, the location of your MagnetEngOffice.pem key, etc.
2. Build mmx or, place the plugin jar file (mmxmgmt*.jar) in mmx/server/plugins/mmxmgmt/target.
3. Run either:
	3.1 installMMXCloud.sh, configure Openfire, and run updateMMXCloud.sh; or, run updateMMXCloud.sh along; or,
	3.2 updateMMXCloud.sh

Instructions for running from Jenkins
-------------------------------------
Use the mmx-develop-all-build_and_deploy

Note
----
1) This will only work with the single install. It hasn’t been tested with cluster install.

Limitation
----------
installMMXCloud.sh is not full-automatic. 2 places require user interaction:
- Updating the amazon ami - it requires user input for timezone, location, accepting java license, etc.
- Initial configuring of Openfire.

Non-issues
----------
ERROR 1396 (HY000) at line 8: Operation DROP USER failed for 'mmx'@'localhost' for prepping the db

sudo: unable to resolve host ip-10-132-254-98  <—- happens when running sudo
