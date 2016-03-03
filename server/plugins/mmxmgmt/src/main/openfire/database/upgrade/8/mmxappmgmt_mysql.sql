UPDATE ofVersion SET version=8 WHERE name = 'mmxappmgmt';

INSERT INTO ofProperty VALUES('mmx.pubsub.notification.type', 'wakeup') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);

INSERT INTO ofProperty VALUES('mmx.apns.pool.max.connections', '100') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty VALUES('mmx.apns.pool.max.app.connections', '20') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty VALUES('mmx.apns.pool.max.idle.count', '1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty VALUES('mmx.apns.pool.wait.sec', '15') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty VALUES('mmx.apns.pool.idle.ttl.min', '10') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);

