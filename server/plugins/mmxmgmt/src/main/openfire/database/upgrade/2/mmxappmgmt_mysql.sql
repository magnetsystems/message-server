
UPDATE ofVersion set version=2 where name = 'mmxappmgmt';

create Table mmxTag (
  id INT NOT NULL AUTO_INCREMENT,
  tagname VARCHAR(25) NOT NULL,
  creationDate DATETIME NOT NULL,
  appid VARCHAR(16) NOT NULL,
  deviceId INT,
  username VARCHAR(64),
  serviceID varchar(100) DEFAULT NULL,
  nodeID varchar(100) DEFAULT NULL,
  PRIMARY KEY(id),
  CONSTRAINT UNIQUE `tagname_appId_deviceId` (tagname, appId, deviceId),
  CONSTRAINT UNIQUE `tagname_appId_username` (tagname, appId, username),
  CONSTRAINT UNIQUE `tagname_appId_topic` (tagname,appid,serviceID, nodeID),
  CONSTRAINT CHECK (deviceId IS NOT NULL OR ofUsername IS NOT NULL OR (serviceID IS NOT NULL AND nodeID IS NOT NULL)),
  FOREIGN KEY (deviceId) REFERENCES mmxDevice(id) ON DELETE CASCADE,
  FOREIGN KEY (username) REFERENCES ofUser(username) ON DELETE CASCADE,
  FOREIGN KEY (serviceID, nodeID) REFERENCES ofPubsubNode(serviceID, nodeID) ON DELETE CASCADE
);

ALTER TABLE mmxDevice ADD COLUMN protocolVersionMajor INT;
ALTER TABLE mmxDevice ADD COLUMN protocolVersionMinor INT;
