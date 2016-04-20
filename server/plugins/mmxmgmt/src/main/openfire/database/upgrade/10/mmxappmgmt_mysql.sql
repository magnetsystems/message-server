UPDATE ofVersion SET version=10 WHERE name = 'mmxappmgmt';

ALTER TABLE mmxWakeupQueue MODIFY payload VARCHAR(2000);
