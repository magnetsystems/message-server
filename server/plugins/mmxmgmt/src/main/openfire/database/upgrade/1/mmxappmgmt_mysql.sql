# $Revision$
# $Date$

UPDATE ofVersion set version=1 where name = 'mmxappmgmt';

/* Add a column to mmxApp table to indicate if the certificate is production or sandbox */
ALTER TABLE mmxApp ADD COLUMN apnsCertProduction tinyint NULL AFTER apnsCertEncryptedPassword;

/* Add a column to the mmxWakeupQueue table for appId. This would allow retrieval of apnsCert and password for the app */
ALTER TABLE mmxWakeupQueue ADD COLUMN appId varchar(16) NULL AFTER tokenType;
