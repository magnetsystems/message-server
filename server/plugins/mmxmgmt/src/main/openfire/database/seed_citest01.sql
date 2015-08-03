# ************************************************************
# Sequel Pro SQL dump
# Version 4096
#
# http://www.sequelpro.com/
# http://code.google.com/p/sequel-pro/
#
# Host: 127.0.0.1 (MySQL 5.6.10)
# Database: openfire
# Generation Time: 2014-10-11 02:19:36 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table mmxApp
# ------------------------------------------------------------

LOCK TABLES `mmxApp` WRITE;
/*!40000 ALTER TABLE `mmxApp` DISABLE KEYS */;

INSERT INTO `mmxApp` (`serverUserId`, `appName`, `appId`, `apiKey`, `encryptedApiKey`, `googleApiKey`, `googleProjectId`, `apnsCert`, `apnsCertPlainPassword`, `apnsCertEncryptedPassword`, `creationDate`, `modificationDate`, `ownerId`,`guestUserId`,`guestSecret`)
VALUES
	('undvrml25hx2%i1sfpss5cmw','unittestapp','i1sfpss5cmw','c9bcd6b9-1e16-434d-a8d3-74d7c52d34d3','8a4ae3d7b511945201999aef5c8b06521d2ecd96c540c84ff41dd3cd94a5cd55c95bf3fcedda6cab7f4f2432a2054f3da2acf5279df63135c080e203994d62af06b8501892ef54af6365628d4e42bded44e28d432155f37c',NULL,NULL,NULL,NULL,NULL,'2014-10-27 23:06:26','2014-10-27 23:06:26','82969470-6119-11e3-84c0-7bfab15be5f6','i1sfpss5cmw%i1sfpss5cmw','foobar');

INSERT INTO `mmxApp` (`serverUserId`, `appName`, `appId`, `apiKey`, `encryptedApiKey`, `googleApiKey`, `googleProjectId`, `apnsCert`, `apnsCertPlainPassword`, `apnsCertEncryptedPassword`, `creationDate`, `modificationDate`, `ownerId`,`guestUserId`,`guestSecret`)
VALUES
	('manager1magnetapi.com%i223hxed420','android-basic','i223hxed420','20fe8256-e121-4252-aeeb-09756e6794aa','c52385ec383d0034b0d200d81cc39e079457aa5883eef8b37359f1a6364a7e7dc8592ba16ebf133e38cd670c79b8564649d30136daa917548ce61df3d2ec9b633149c14f1b4b85afc418cf4604a2c2460624fcf9470f49ac','AIzaSyDYVjCGwLXDn_ChatnbePadt5GMp_LxpFM','599981932022',NULL,NULL,NULL,'2014-11-03 17:22:06','2014-11-03 17:25:49','82969470-6119-11e3-84c0-7bfab15be5f6','i223hxed420%i223hxed420','nkun1m0qiwio');

INSERT INTO `mmxApp` (`serverUserId`, `appName`, `appId`, `apiKey`, `encryptedApiKey`, `googleApiKey`, `googleProjectId`, `apnsCert`, `apnsCertPlainPassword`, `apnsCertEncryptedPassword`, `creationDate`, `modificationDate`, `ownerId`,`guestUserId`,`guestSecret`)
VALUES
	('manager1magnetapi.com%gkhi369rspz','Functional Test','gkhi369rspz','b2069100-421e-4156-8485-e026d441ac76','c1363c65cd8157a07de751084c3ec0a8d6d4b92e4a05256ad4d3db7440672dfe11e4ef14770eb63259c7b65a030a92e8c2dc8127cf76bc89f74c70ff5821a3eedad54f0cf81ec289c9bc2df11c8ba4f69e8ddcd0e9c64a88','AIzaSyDYVjCGwLXDn_ChatnbePadt5GMp_LxpFM','599981932022',NULL,NULL,NULL,'2014-12-01 20:08:31','2014-12-01 20:27:37','82969470-6119-11e3-84c0-7bfab15be5f6','gkhi369rspz%gkhi369rspz','-j0wtpkh570st');

/*!40000 ALTER TABLE `mmxApp` ENABLE KEYS */;

UNLOCK TABLES;


LOCK TABLES 'ofPubsubNode' WRITE;

DELETE ofPubsubNode WHERE serviceID='pubsub' AND nodeID='gkhi369rspz';

INSERT INTO 'ofPubsubNode' ('serviceID', 'nodeID', 'leaf', 'createDate', 'modificationDate', 'deliverPayloads', 'maxPayloadSize', 'persistItems', 'maxItems', 'creator', 'language', 'associationPolicy', 'maxLeafNodes')
VALUES
  ("pubsub", "gkhi369rspz", 0, "001425694254715", "001425694254715", 1, 0, 0, 0, "server-user%gkhi369rspz@magnet-linux", "English", "all", -1);

UNLOCK TABLES;


/*
LOCK TABLES `ofUser` WRITE;

INSERT INTO ofUser( username, encryptedPassword, name, creationDate, modificationDate)
VALUES
  ("i1sfpss5cmw%i1sfpss5cmw", "ccd053b66b7ac024446271c6750f000c7361a4ead8be3afbf507f5582de7f087dbc5a0b625d5f43d164d39cbc4838027f31e8a01ac7f69d82ff6522bf6c943dd8ab6f73549a706dd", "I1sfpss5cmw", "001414451186955", "001414451186955");

INSERT INTO ofUser( username, encryptedPassword, name, creationDate, modificationDate)
VALUES
  ("undvrml25hx2%i1sfpss5cmw", "0d6a5253434518a5dae9321de28ab6ada0145c37f6b45a8e53258625a31feae5da01f800bf6649d5", "Undvrml25hx2", "001414451186938", "001414451186938");

INSERT INTO ofUser (username, encryptedPassword, name, creationDate, modificationDate)
VALUES
  ("manager1magnetapi.com%i223hxed420", "72550e213930a23a8ad0347cc6155a78f783629579c92232a385f150b9469d9c6b820587d3e1cff2", "Manager1magnetapi Com", "001414095547796", "001414095547796");

INSERT INTO ofUser (username, encryptedPassword, name, creationDate, modificationDate)
VALUES
  ("i223hxed420%i223hxed420", "78277fd7948e3262218ced7413cd163995892ced7ea787fb88a9c583ad840f6cf83284bc1d1d7326177dd53992029e051be28f0e0deb6cfa71440fd44b58b0dcdac5c302cc594c8f", "I223hxed420", "001415035326046", "001415035326046");

UNLOCK TABLES;
*/


/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
