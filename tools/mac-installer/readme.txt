Tool used: http://s.sudre.free.fr/Software/Packages/about.html

Packages project file:
MMS/MMS.pkgproj 

Build prerequisite:
1. Install the Developer ID cert in the Apple Keychain Access on the build machine.
2. Make sure that the project is able to access it by:
2.1. Installing Packages
2.2. Open the project. On the Settings page of Project (not the package), there should be a “CERTIFICATE OF AUTHENTICITY” stamp. If not, go to Project/Set Certificate and choose the Developer ID Installer certificate. 
3. Install the "Developer ID Distribution.p12" into Keychain Access.
4. Run the script the first time with attendance as it requires user interaction. It will ask packagesbuild wants to sign using key "privateKey" in your keychain. Click "Always Allow".

Build Instructions:
Run build_pkg.sh on the Jenkins slave

Note:
Packages requires to have a definite payload folder name; therefore, whenever there is a change in the folder name (esp. because of the change of version number), the project needs to be updated.

MMX version change:
Whenever there is a version change mmx, a few places need to be updated: 
1. Packages/Project/Presentation/Title
2. PACKAGES/Magnet Messaging/Payload/Contents//Users/Shared/mmx-standalone-dist (delete the folder and add again)
3. The sh scripts inside resources.
