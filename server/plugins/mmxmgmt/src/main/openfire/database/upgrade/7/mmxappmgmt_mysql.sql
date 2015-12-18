UPDATE ofVersion SET version=7 WHERE name = 'mmxappmgmt';

/* Update the database to support utf8mb4 for emoji */
ALTER DATABASE magnetmmxdb CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

/* Support utf8mb4 for emoji in messages; application and device names are not suppored */
ALTER TABLE ofOffline MODIFY stanza MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE ofPubsubItem MODIFY payload MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE ofPubsubNode MODIFY description VARCHAR(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

REPAIR TABLE ofOffline;
OPTIMIZE TABLE ofOffline;

REPAIR TABLE ofPubsubItem;
OPTIMIZE TABLE ofPubsubItem;

REPAIR TABLE ofPubsubNode;
OPTIMIZE TABLE ofPubsubNode;
