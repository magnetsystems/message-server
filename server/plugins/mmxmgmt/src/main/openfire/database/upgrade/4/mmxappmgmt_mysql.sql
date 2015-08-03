UPDATE ofVersion SET version=4 WHERE name = 'mmxappmgmt';

/* Table for app specific configuration */
CREATE TABLE mmxAppConfiguration (
    id          INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appId       VARCHAR(16)     NOT NULL,
    configKey   VARCHAR(100)    NOT NULL,
    configValue VARCHAR(300)    NOT NULL,
    FOREIGN KEY (appId) REFERENCES mmxApp(appId) ON DELETE CASCADE
);

ALTER TABLE mmxAppConfiguration ADD UNIQUE KEY `mmxAppConfiguration_uk` (appId, configKey);

DELETE FROM ofProperty WHERE name = 'mmx.instance.max.inapp.message.rate.per.sec';
DELETE FROM ofProperty WHERE name = 'mmx.instance.max.push.message.rate.per.sec';

INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.xmpp.rate.per.sec', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.http.rate.per.sec', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
