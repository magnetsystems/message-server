UPDATE ofVersion SET version=11 WHERE name = 'mmxappmgmt';

DELETE FROM ofProperty WHERE name='mmx.pubsub.notification.type';

ALTER TABLE mmxTemplate ADD COLUMN ownerId VARCHAR(64) NULL;
ALTER TABLE mmxTemplate ADD COLUMN creationDate DATETIME NULL;
ALTER TABLE mmxTemplate ADD COLUMN modificationDate DATETIME NULL;
