UPDATE ofVersion SET version=6 WHERE name = 'mmxappmgmt';

/* Allow pubsub to send item to all devices of a user */
INSERT INTO ofProperty VALUES('route.all-resources', 'true') ON DUPLICATE KEY UPDATE propValue='true';

