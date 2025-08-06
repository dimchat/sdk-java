# Decentralized Instant Messaging (Java SDK)


[![License](https://img.shields.io/github/license/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dimchat/sdk-java/pulls)
[![Platform](https://img.shields.io/badge/Platform-Java%208-brightgreen.svg)](https://github.com/dimchat/sdk-java/wiki)
[![Issues](https://img.shields.io/github/issues/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/issues)
[![Repo Size](https://img.shields.io/github/repo-size/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/archive/refs/heads/master.zip)
[![Tags](https://img.shields.io/github/tag/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/tags)
[![Version](https://img.shields.io/maven-central/v/chat.dim/SDK)](https://mvnrepository.com/artifact/chat.dim/SDK)

[![Watchers](https://img.shields.io/github/watchers/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/watchers)
[![Forks](https://img.shields.io/github/forks/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/forks)
[![Stars](https://img.shields.io/github/stars/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/stargazers)
[![Followers](https://img.shields.io/github/followers/dimchat)](https://github.com/orgs/dimchat/followers)

## Dependencies

* Latest Versions

| Name | Version | Description |
|------|---------|-------------|
| [Ming Ke Ming (名可名)](https://github.com/dimchat/mkm-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/MingKeMing)](https://mvnrepository.com/artifact/chat.dim/MingKeMing) | Decentralized User Identity Authentication |
| [Dao Ke Dao (道可道)](https://github.com/dimchat/dkd-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/DaoKeDao)](https://mvnrepository.com/artifact/chat.dim/DaoKeDao) | Universal Message Module |
| [DIMP (去中心化通讯协议)](https://github.com/dimchat/core-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/DIMP)](https://mvnrepository.com/artifact/chat.dim/DIMP) | Decentralized Instant Messaging Protocol |

* build.gradle

```javascript
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}

dependencies {
    // https://central.sonatype.com/artifact/chat.dim/SDK
    implementation group: 'chat.dim', name: 'SDK', version: '2.0.0'
}
```

* pom.xml

```xml
<dependencies>

    <!-- https://mvnrepository.com/artifact/chat.dim/SDK -->
    <dependency>
        <groupId>chat.dim</groupId>
        <artifactId>SDK</artifactId>
        <version>2.0.0</version>
        <type>pom</type>
    </dependency>

</dependencies>
```

## Extensions

### Content

extends [CustomizedContent](https://github.com/dimchat/core-java#extends-content)

### ContentProcessor

```java
import java.util.HashMap;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.app.CustomizedContentHandler;
import chat.dim.protocol.CustomizedContent;
import chat.dim.protocol.ReliableMessage;


/**
 *  Customized Content Processing Unit
 *  <p>
 *      Handle content for application customized
 *  </p>
 */
public class AppCustomizedProcessor extends CustomizedContentProcessor {

    private final Map<String, CustomizedContentHandler> handlers = new HashMap<>();

    public AppCustomizedProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    public void setHandler(String app, String mod, CustomizedContentHandler handler) {
        handlers.put(app + ":" + mod, handler);
    }

    protected CustomizedContentHandler getHandler(String app, String mod) {
        return handlers.get(app + ":" + mod);
    }

    @Override
    protected CustomizedContentHandler filter(String app, String mod, CustomizedContent content, ReliableMessage rMsg) {
        CustomizedContentHandler handler = getHandler(app, mod);
        if (handler != null) {
            return handler;
        }
        // default handler
        return super.filter(app, mod, content, rMsg);
    }

}
```

```java
import java.util.List;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.*;
import chat.dim.protocol.group.*;


/*  Command Transform:

    +===============================+===============================+
    |      Customized Content       |      Group Query Command      |
    +-------------------------------+-------------------------------+
    |   "type" : i2s(0xCC)          |   "type" : i2s(0x88)          |
    |   "sn"   : 123                |   "sn"   : 123                |
    |   "time" : 123.456            |   "time" : 123.456            |
    |   "app"  : "chat.dim.group"   |                               |
    |   "mod"  : "history"          |                               |
    |   "act"  : "query"            |                               |
    |                               |   "command"   : "query"       |
    |   "group"     : "{GROUP_ID}"  |   "group"     : "{GROUP_ID}"  |
    |   "last_time" : 0             |   "last_time" : 0             |
    +===============================+===============================+
 */
public final class GroupHistoryHandler extends BaseCustomizedHandler {

    public GroupHistoryHandler(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> handleAction(String act, ID sender, CustomizedContent content, ReliableMessage rMsg) {
        if (content.getGroup() == null) {
            assert false : "group command error: " + content + ", sender: " + sender;
            return respondReceipt("Group command error.", rMsg.getEnvelope(), content, null);
        }
        if (GroupHistory.ACT_QUERY.equals(act)) {
            assert GroupHistory.APP.equals(content.getApplication());
            assert GroupHistory.MOD.equals(content.getModule());
            return transformQueryCommand(content, rMsg);
        }
        assert false : "unknown action: " + act + ", " + content + ", sender: " + sender;
        return super.handleAction(act, sender, content, rMsg);
    }

    private List<Content> transformQueryCommand(CustomizedContent content, ReliableMessage rMsg) {
        Messenger messenger = getMessenger();
        if (messenger == null) {
            assert false : "messenger lost";
            return null;
        }
        Map<String, Object> info = content.copyMap(false);
        info.put("type", ContentType.COMMAND);
        info.put("command", GroupCommand.QUERY);
        Content query = Content.parse(info);
        if (query instanceof QueryCommand) {
            return messenger.processContent(query, rMsg);
        }
        assert false : "query command error: " + query + ", " + content + ", sender: " + rMsg.getSender();
        return respondReceipt("Query command error.", rMsg.getEnvelope(), content, null);
    }

}
```

### ContentProcessorCreator

```java
import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.app.GroupHistoryHandler;
import chat.dim.dkd.ContentProcessor;
import chat.dim.protocol.*;
import chat.dim.protocol.group.GroupHistory;

public class ClientContentProcessorCreator extends BaseContentProcessorCreator {

    public ClientContentProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    protected AppCustomizedProcessor createCustomizedContentProcessor(Facebook facebook, Messenger messenger) {
        AppCustomizedProcessor cpu = new AppCustomizedProcessor(facebook, messenger);

        // 'chat.dim.group:history'
        cpu.setHandler(
                GroupHistory.APP,
                GroupHistory.MOD,
                new GroupHistoryHandler(facebook, messenger)
        );

        return cpu;
    }

    @Override
    public ContentProcessor createContentProcessor(String msgType) {
        switch (msgType) {

            // application customized
            case ContentType.APPLICATION:
            case "application":
            case ContentType.CUSTOMIZED:
            case "customized":
                return createCustomizedContentProcessor(getFacebook(), getMessenger());

            // ...
        }
        // others
        return super.createContentProcessor(msgType);
    }

    @Override
    public ContentProcessor createCommandProcessor(String type, String name) {
        switch (name) {

            case HandshakeCommand.HANDSHAKE:
                return new HandshakeCommandProcessor(getFacebook(), getMessenger());

            // ...
        }
        // others
        return super.createCommandProcessor(type, name);
    }
}
```

### ExtensionLoader

```java
import chat.dim.dkd.AppCustomizedContent;
import chat.dim.plugins.ExtensionLoader;
import chat.dim.protocol.*;


/**
 *  Extensions Loader
 *  ~~~~~~~~~~~~~~~~~
 */
public class CommonExtensionLoader extends ExtensionLoader {

    @Override
    protected void registerCustomizedFactories() {

        // Application Customized
        setContentFactory(ContentType.CUSTOMIZED, "customized", AppCustomizedContent::new);
        setContentFactory(ContentType.APPLICATION, "application", AppCustomizedContent::new);

        //super.registerCustomizedFactories();
    }

    /**
     *  Command factories
     */
    @Override
    protected void registerCommandFactories() {
        super.registerCommandFactories();

        // Handshake
        setCommandFactory(HandshakeCommand.HANDSHAKE, HandshakeCommand::new);

    }

}
```

## Usages

You must load all extensions before your business run:

```java
import chat.dim.plugins.ExtensionLoader;
import chat.dim.plugins.PluginLoader;


public class LibraryLoader implements Runnable {

    private final ExtensionLoader extensionLoader;
    private final PluginLoader pluginLoader;

    public LibraryLoader(ExtensionLoader extensionLoader, PluginLoader pluginLoader) {

        if (extensionLoader == null) {
            this.extensionLoader = new CommonExtensionLoader();
        } else {
            this.extensionLoader = extensionLoader;
        }

        if (pluginLoader == null) {
            this.pluginLoader = new PluginLoader();
        } else {
            this.pluginLoader = pluginLoader;
        }
    }

    @Override
    public void run() {
        extensionLoader.run();
        pluginLoader.run();
    }

    public static void main(String[] args) {
    
        LibraryLoader loader = new LibraryLoader();
        loader.run();
        
        // do your jobs after all extensions loaded.
        
    }

}
```

To let your **AppCustomizedProcessor** start to work,
you must override ```BaseContentProcessorCreator``` for message types:

1. ContentType.APPLICATION 
2. ContentType.CUSTOMIZED

and then set your **creator** for ```GeneralContentProcessorFactory``` in the ```MessageProcessor```.

----

Copyright &copy; 2018-2025 Albert Moky
[![Followers](https://img.shields.io/github/followers/moky)](https://github.com/moky?tab=followers)