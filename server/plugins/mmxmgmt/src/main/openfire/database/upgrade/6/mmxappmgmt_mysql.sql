UPDATE ofVersion SET version=6 WHERE name = 'mmxappmgmt';

/* Force the domain to be "mmx" */
INSERT INTO ofProperty VALUES('xmpp.domain', 'mmx') ON DUPLICATE KEY UPDATE propValue='mmx';
/* Allow pubsub to send item to all devices of a user */
INSERT INTO ofProperty VALUES('route.all-resources', 'true') ON DUPLICATE KEY UPDATE propValue='true';

