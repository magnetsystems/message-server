#This contains all the db schema changes that are going to be applied every time the server is deployed
delete from ofProperty where name='provider.lockout.className';
insert into ofProperty values ('provider.lockout.className', 'com.magnet.mmx.lockout.MmxLockoutProvider');

delete from ofProperty where name='xmpp.parser.buffer.size';
insert into ofProperty values ('xmpp.parser.buffer.size', '2097152');

delete from ofProperty where name='xmpp.routing.strict';
insert into ofProperty values('xmpp.routing.strict', 'true');

delete from ofProperty where name='xmpp.client.idle';
insert into ofProperty values('xmpp.client.idle', '-1');

delete from ofProperty where name='xmpp.client.idle.ping';
insert into ofProperty values('xmpp.client.idle.ping', 'false');

delete from ofProperty where name='route.all-resources';
insert into ofProperty values('route.all-resources', 'true');

delete from ofProperty where name='xmpp.proxy.enabled';
insert into ofProperty values('xmpp.proxy.enabled', 'false');

delete from ofProperty where name='xmpp.auth.anonymous';
insert into ofProperty values('xmpp.auth.anonymous', 'false');
