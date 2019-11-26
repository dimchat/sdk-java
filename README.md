# Decentralized Instant Messaging (Java SDK)


[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/dimchat/sdk-java/blob/master/LICENSE)
[![Version](https://img.shields.io/badge/alpha-0.2.2-red.svg)](https://github.com/dimchat/sdk-java/archive/master.zip)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dimchat/sdk-java/pulls)
[![Platform](https://img.shields.io/badge/Platform-Java%208-brightgreen.svg)](https://github.com/dimchat/sdk-java/wiki)


### Dependencies

build.gradle

```javascript
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}

dependencies {
    // https://bintray.com/dimchat/common/sdk
    implementation group: 'chat.dim', name: 'SDK', version: '0.2.2'
}
```

pom.xml

```xml
<dependencies>

    <!-- https://mvnrepository.com/artifact/chat.dim/SDK -->
    <dependency>
        <groupId>chat.dim</groupId>
        <artifactId>SDK</artifactId>
        <version>0.2.2</version>
        <type>pom</type>
    </dependency>

</dependencies>
```


## Account

User private key, ID, meta, and profile are generate in client,
and broadcast only ```meta``` & ```profile``` onto DIM station.

### Register User Account

Step 1. generate private key (with asymmetric algorithm)

```java
PrivateKey privateKey = PrivateKeyImpl.generate(PrivateKey.RSA);
```

Step 2. generate meta with private key (and meta seed)

```java
String seed = "username";
Meta = Meta.generate(MetaType.Default, privateKey, seed);
```

Step 3. generate ID with meta (and network type)

```java
ID identifier = meta.generateID(NetworkType.Main);
```

### Create and upload User Profile

Step 4. create profile with ID and sign with private key

```java
UserProfile profile = new UserProfile(identifier);
// set nickname and avatar URL
profile.setName("Albert Moky");
profile.setAvatar("https://secure.gravatar.com/avatar/34aa0026f924d017dcc7a771f495c086");
// sign
profile.sign(privateKey);
```

Step 5. upload meta & profile

```java
Messenger messenger = Messenger.getInstance();

Command cmd = new ProfileCommand(identifier, meta, profile);
messenger.sendCommand(cmd);
```

The profile should be sent to station after connected
and handshake accepted, details are provided in later chapters

## Connect and Handshake

Step 1. connect to DIM station (TCP)

Step 2. prepare for receiving message data package

```java
public void onReceive(byte[] responseData) {
    byte[] response = messenger.receivedPackage(responseData);
    if (response != null && response.length > 0) {
        // send processing result back to the station
        send(response);
    }
}
```

Step 3. send first **handshake** command

(1) create handshake command

```java
// first handshake will have no session key
String sessionKey = null;
Command cmd = new HandshakeCommand(sessionKey);
```

(2) pack, encrypt and sign

```java
InstantMessage iMsg = new InstantMessage(cmd, userId, stationId);
SecureMessage sMsg = messenger.encryptMessage(iMsg);
ReliableMessage rMsg = messenger.signMessage(sMsg);
```

(3) Meta protocol

Attaching meta in the first message package is to make sure the station will not lost it,
when it's first time the user connect to this station.

```java
// [Meta protocol]
//      
//      to avoid the station cannot find it,
//      if 
rMsg.setMeta(user.getMeta());
```

(4) send out serialized message data package

```java
byte[] data = messenger.serializeMessage(rMsg);
send(data);
```

Step 4. waiting handshake response

The CPU (Command Processing Units) will catch the handshake command respond from station, and CPU will process them automatically, so just wait untill handshake success or network error.

## Message

### Content

* Text message

```java
Content content = new TextContent("Hey, girl!");
```

* Image message

```java
Content content = new ImageContent(imageData, "image.png");
```

* Voice message

```java
Content content = new AudioContent(voiceData, "voice.mp3");
```

**NOTICE**: file message content (includes Image, Audio, Video)
will be sent out only includes the filename and a URL
where the file data (encrypted with the same symmetric key) be stored.

### Command

* Query meta with contact ID

```java
Command cmd = new MetaCommand(identifier);
```

* Query profile with contact ID

```java
Command cmd = new ProfileCommand(identifier);
```

### Send command

```java
public class MyMessenger extends chat.dim.Messenger {

    /**
     *  Pack and send command to station
     *
     * @param cmd - command
     * @return true on success
     */
    public boolean sendCommand(Command cmd) {
        assert server != null;
        return sendContent(cmd, server.identifier);
    }
}
```

MetaCommand or ProfileCommand with only ID means querying, and the CPUs will catch and process all the response automatically.

## Command Processing Units

You can send a customized command (such as **search command**) and prepare a processor to handle the response.

### Search command processor

```java
public class SearchCommandProcessor extends CommandProcessor {

    public static final String SearchUpdated = "SearchUpdated";

    public SearchCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private void parse(SearchCommand cmd) {
        Map<String, Object> results = cmd.getResults();
        // TODO: processing results
    }

    @Override
    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof SearchCommand;
        parse((SearchCommand) content);

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(SearchUpdated, this, content);
        // response nothing to the station
        return null;
    }
}
```

### Handshake command processor

```java
public class HandshakeCommandProcessor extends CommandProcessor {

    public HandshakeCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content success() {
        Log.info("handshake success!");
        String sessionKey = (String) getContext("session_key");
        Server server = (Server) getContext("server");
        server.handshakeAccepted(sessionKey, true);
        return null;
    }

    private Content ask(String sessionKey) {
        Log.info("handshake again, session key: " + sessionKey);
        setContext("session_key", sessionKey);
        return new HandshakeCommand(sessionKey);
    }

    @Override
    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof HandshakeCommand;
        HandshakeCommand cmd = (HandshakeCommand) content;
        String message = cmd.message;
        if ("DIM!".equals(message)) {
            // S -> C
            return success();
        } else if ("DIM?".equals(message)) {
            // S -> C
            return ask(cmd.sessionKey);
        } else {
            // C -> S: Hello world!
            throw new IllegalStateException("handshake command error: " + content);
        }
    }
}
```

And don't forget to register them.

```java
CommandProcessor.register(Command.HANDSHAKE, HandshakeCommandProcessor.class);
CommandProcessor.register(SearchCommand.SEARCH, SearchCommandProcessor.class);
```

Copyright &copy; 2019 Albert Moky
