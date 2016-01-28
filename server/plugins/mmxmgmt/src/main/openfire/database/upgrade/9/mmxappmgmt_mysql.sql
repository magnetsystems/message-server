UPDATE ofVersion SET version=9 WHERE name = 'mmxappmgmt';

ALTER TABLE mmxApp MODIFY apnsCert VARBINARY(65535);
