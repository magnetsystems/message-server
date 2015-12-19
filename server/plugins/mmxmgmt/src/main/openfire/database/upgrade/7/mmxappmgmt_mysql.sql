UPDATE ofVersion SET version=7 WHERE name = 'mmxappmgmt';

/* Support utf8mb4 for emoji in pubsub items and offline messages. */
/* Application and device names are not suppored; let MMS handle them. */
ALTER TABLE ofOffline MODIFY stanza MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE ofPubsubItem MODIFY payload MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE ofPubsubNode MODIFY description VARCHAR(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
