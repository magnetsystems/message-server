The MmxMgmt servlet allows an authorized user to perform app management requests via a web browser or using cRUL commands. 


- App Creation: creating an app based on app name, owner, server user, server user password, google api key, google project id,
  path to APNS certificate, certificate password. The tool returns app key and api key after successful creation of
  the app. 

- Listing apps: Listing all apps created by the owner. Value of the "owner" is passed in by the caller and should be the same value as the one used for app creation

- App Modification: identifying an app by app key and modifying app name, google api key, google project id,
  APNS certificate, and certificate password. 
  
- Deletion: deleting an app by providing the app key of the app.
 
The user credential can be specified via basic authroization header or using query parameters.
 
The following are the parameters passed to the requests:
 
 Credentials(Basic-Auth)
 userName:        Openfire administrator user
 password:        password to userName
 
 command:         Required. Possible values are create, delete, update and read.
 appName:         Required for create, optional for the other commands.
 appId:          Required for read, update, delete
 appOwner:        Required for create, list apps.
 serverUser:      Optional. If not specified, a server user will be generated. The full server user is <serverUser>%<appId>@domain.
 serverPassword:       Optional. If not specified, a password for server user will be generated.
 googleApiKey:    Optional. Google API Key for the app.
 googleProjectId: Optional. Google Project ID for the app.
 apnsCertPath:    Optional. Path to the APNS certificate.
 apnsPwd:         Optional. Password of the APNS certificate.
 

 
Pre-conditions
--------------

The Openfire server must be running with mmxmgmt plugin installed. You must have administrator Openfire account to access the MmxMgmt server.


Sample HTML requests
--------------------

The following are sample HTML requests issued via a web browser or cRUL.

LIST APPS
---------

cRUL:

curl -i -u user1:test http://example.com:9090/plugins/mmxmgmt -d "command=read&appOwner=owner"

Browser:

http://example.com:9090/plugins/mmxmgmt?userName=user1&password=test&command=read&appOwner=owner


READ APP
--------

cRUL:

curl -i -u user1:test http://example.com:9090/plugins/mmxmgmt -d "command=read&appId=appKeyValue"

Browser:

http://example.com:9090/plugins/mmxmgmt?userName=user1&password=test&command=read&&appId=appKeyValue


CREATE APPS
-----------

cURL:

curl -u user1:test http://example.com:9090/plugins/mmxmgmt -d "command=create&appName=app1&appOwner=owner"

Browser:

http://example.com:9090/plugins/mmxmgmt?userName=user1&password=test&command=create&appName=app1&appOwner=owner


UPDATE APPS
-----------
*** Change the GCM keys:


curl -u user1:test http://example.com:9090/plugins/mmxmgmt -d "command=update&appKey=e03e7136-dca6-41f0-96c8-958d9208a9fc&googleApiKey=AIzaSyDYVjCGwLXDn_ChatnbePadt5GMp_LxpFM&googleProjectId=599981932022

*** Change the appName:

cURL:

curl -u user1:test http://example.com:9090/plugins/mmxmgmt -d "command=update&appKey=e03e7136-dca6-41f0-96c8-958d9208a9fc&appName=app1Updated"

Browser:

http://example.com:9090/plugins/mmxmgmt?userName=user1&password=test&command=update&appKey=e03e7136-dca6-41f0-96c8-958d9208a9fc&appName=app1Updated




DELETE APPS
-----------

cURL:

curl -u user1:test http://example.com:9090/plugins/mmxmgmt -d "command=delete&appKey=f22287d7-a297-41b8-8cbd-d1237c56ecfc"

Browser:

http://example.com:9090/plugins/mmxmgmt?userName=user1&password=test&command=delete&appKey=f22287d7-a297-41b8-8cbd-d1237c56ecfc



