How to run:

1) Edit the configuration file config.

   The config file gives you an easy way to define an environment For eg : localhost, citest etc
   Populate the fields with the correct configuration parameters by looking at the console.
   "numUsers" defines the number of users used in the test. Ideally keep it an even number so that
   pairs of users will send messages to each other. "messagesPerUser" defines the numbers of messages to be
   sent by every user.

configs:
 - name: citest
   mmx:
     host: 207.135.69.242
     port: 5222
     apiKey: 969115d7-202b-45a7-8f94-1cf98cb351ad
     appId: 5ifi5spfnmc
     guestSecret: e9744010-9e3c-4bc5-876c-220e3560922f
     guestUserId: 5ifi5spfnmc
     appPath: /Users/sdatar/work/git/mmxpinger/src/main/resources/
     enableSendLastPubItem: false
   load:
     prefix: loaduser
     numUsers: 2
     messagesPerUser: 1
     sendReceipt: true
     password: 1234

 - name: localhost
   mmx:
     host: localhost
     port: 5222
     apiKey: 7e8f99da-266b-4079-9db6-1ceaa06b5fca
     appId: 9u3i8aukna2
     guestSecret: 65ea56eb-98b6-4ef7-9139-4c596a81d59c
     guestUserId: 9u3i8aukna2
     appPath: /Users/sdatar/work/git/mmxpinger/src/main/resources/
     enableSendLastPubItem: false
   load:
     prefix: magnetuser
     numUsers: 30
     messagesPerUser: 10
     interMessageDelay: 1000
     sendReceipt: true
     password: 1234


2) mvn clean install
    Build the project

3) mvn exec:exec
    Runs the test

To switch environments edit pom.xml and set -Denv argument to the desired environment
