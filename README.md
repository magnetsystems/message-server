Magnet Message Server
========

About
-----
Message Server repository contains the source code for Magnet's Openfire plugin and auxiliary build and test tools. The plugin together with Magnet's customized version of Openfire constitutes the Magnet Message server. This repository also contains build tools for creating installation packages for supported platforms (Linux, Mac OSX and Windows).
To try out Magnet Message server in a sandbox environment please go to Magnet's [cloud hosted sandbox](https://sandbox.magnet.com)

### Build Instructions
----------------------

#### Pre-requisites
- JDK 1.6 or above.
- Maven 3 or above.

1. `git clone https://github.com/magnetsystems/message-server`
2. `mvn clean install`

The build will generate the following installation packages:

1. Linux / Mac OS X installer for standalone mode:
`./tools/mmx-standalone-dist/target/mmx-standalone-dist-{version}.zip`
2. Linux / Mac OS X installer for clustered mode:
`./tools/mmx-server-cluster-zip/target/mmx-server-cluster-{version}.zip`
3.Windows installer for standalone mode:
`./tools/mmx-standalone-dist-win/target/mmx-standalone-dist-win-{version}.zip`

There are README files in each of the individual packages for server installation instructions.

### Note
For detailed installation instructions for your favorite platform, please check (Magnet's developer hub)[https://docs.magnet.com/message/local-installation/]


