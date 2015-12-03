UPDATE ofVersion SET version=5 WHERE name = 'mmxappmgmt';

/* Table for mapping topics to roles that are allowed access to the topic */
CREATE TABLE mmxTopicRole (
    id           INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    serviceID    VARCHAR(100)    NOT NULL,
    nodeID       VARCHAR(100)    NOT NULL,
    role         VARCHAR(100)    NOT NULL,
    creationDate datetime        NOT NULL,
    FOREIGN KEY (serviceID, nodeID) REFERENCES ofPubsubNode(serviceID, nodeID) ON DELETE CASCADE
);

ALTER TABLE mmxTopicRole ADD UNIQUE KEY `mmxTopicRole_uk` (serviceID, nodeID, role);

INSERT INTO mmxTopicRole (serviceID, nodeID, role, creationDate)
SELECT serviceID,nodeID,'PUBLIC', now() FROM ofPubsubNode WHERE nodeID IS NOT NULL;

/* Allow pubsub to send item to all devices of a user */
INSERT INTO ofProperty VALUES('route.all-resources', 'true') ON DUPLICATE KEY UPDATE propValue='true';

