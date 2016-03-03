UPDATE ofVersion SET version=9 WHERE name = 'mmxappmgmt';

# The column size is limited by the max row size (65535).
ALTER TABLE mmxApp MODIFY apnsCert VARBINARY(32768);
