UPDATE ofVersion set version=6 where name = 'mmxappmgmt';

INSERT INTO ofProperty VALUES('route.all-resources', 'true') ON DUPLICATE KEY UPDATE propValue='true';
