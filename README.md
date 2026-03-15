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
| [Cryptography](https://github.com/dimchat/mkm-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/Crypto)](https://mvnrepository.com/artifact/chat.dim/Crypto) | Crypto Keys |
| [Ming Ke Ming (名可名)](https://github.com/dimchat/mkm-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/MingKeMing)](https://mvnrepository.com/artifact/chat.dim/MingKeMing) | Decentralized User Identity Authentication |
| [Dao Ke Dao (道可道)](https://github.com/dimchat/dkd-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/DaoKeDao)](https://mvnrepository.com/artifact/chat.dim/DaoKeDao) | Universal Message Module |
| [DIMP (去中心化通讯协议)](https://github.com/dimchat/core-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/DIMP)](https://mvnrepository.com/artifact/chat.dim/DIMP) | Decentralized Instant Messaging Protocol |

## Extensions

### Content

extends [CustomizedContent](https://github.com/dimchat/core-java#extends-content)

### ContentProcessor

```java
/**
 *  Customized Content Processing Unit
 *  <p>
 *      Handle content for application customized
 *  </p>
 */
public class CustomizedContentProcessor extends BaseContentProcessor {

    public CustomizedContentProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        assert content instanceof CustomizedContent : "customized content error: " + content;
        CustomizedContent customized = (CustomizedContent) content;
        CustomizedContentFilter filter = SharedCustomizedFilter.customizedFilter;
        // get handler for 'app' & 'mod'
        CustomizedContentHandler handler = filter.filterContent(customized, rMsg);
        if (handler == null) {
            assert false : "should not happen";
            return null;
        }
        // handle the action
        Messenger messenger = getMessenger();
        return handler.handleContent(customized, rMsg, messenger);
    }

}
```

- CustomizedContentHandler


```java
/**
 *  Handler for Customized Content
 */
public interface CustomizedContentHandler {

    /**
     *  Do your job
     *
     * @param content   - customized content
     * @param rMsg      - network message
     * @param messenger - message transceiver
     * @return responses
     */
    List<Content> handleContent(CustomizedContent content, ReliableMessage rMsg, Messenger messenger);
}
```

```java
/**
 *  Default Handler
 */
public class BaseCustomizedContentHandler implements CustomizedContentHandler {

    public BaseCustomizedContentHandler() {
        super();
    }

    @Override
    public List<Content> handleContent(CustomizedContent content, ReliableMessage rMsg, Messenger messenger) {
        //String app = content.getApplication();
        String app = content.getString("app");
        String mod = content.getModule();
        String act = content.getAction();
        return respondReceipt("Content not support.", rMsg.getEnvelope(), content, TwinsHelper.newMap(
                "template", "Customized content (app: ${app}, mod: ${mod}, act: ${act}) not support yet!",
                "replacements", TwinsHelper.newMap(
                        "app", app,
                        "mod", mod,
                        "act", act
                )
        ));
    }

    //
    //  Convenient responding
    //

    protected List<Content> respondReceipt(String text, Envelope envelope, Content content, Map<String, Object> extra) {
        // create base receipt command with text & original envelope
        ReceiptCommand res = BaseContentProcessor.createReceipt(text, envelope, content, extra);
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }

}
```

- CustomizedContentFilter

```java
/**
 *  Filter for Customized Content Handler
 */
public interface CustomizedContentFilter {

    /**
     *  Fetch a handler for 'app' and 'mod'
     *
     * @param content - customized content
     * @param rMsg    - message with envelope
     * @return customized handler
     */
    CustomizedContentHandler filterContent(CustomizedContent content, ReliableMessage rMsg);

}
```

```java
/**
 *  CustomizedContent Extensions
 */
public final class SharedCustomizedFilter {

    public static CustomizedContentFilter customizedFilter = new AppCustomizedFilter();

}
```

```java
/**
 *  General CustomizedContent Filter
 */
public class AppCustomizedFilter implements CustomizedContentFilter {

    private final Map<String, CustomizedContentHandler> handlers;

    private final CustomizedContentHandler defaultHandler;

    public AppCustomizedFilter() {
        super();
        handlers = new HashMap<>();
        defaultHandler = new BaseCustomizedContentHandler();
    }

    public void setContentHandler(String app, String mod, CustomizedContentHandler handler) {
        handlers.put(app + ":" + mod, handler);
    }

    protected CustomizedContentHandler getContentHandler(String app, String mod) {
        return handlers.get(app + ":" + mod);
    }

    @Override
    public CustomizedContentHandler filterContent(CustomizedContent content, ReliableMessage rMsg) {
        //String app = content.getApplication();
        String app = content.getString("app");
        String mod = content.getModule();
        CustomizedContentHandler handler = getContentHandler(app, mod);
        if (handler != null) {
            return handler;
        }
        // if the application has too many modules, I suggest you to
        // use different handler to do the jobs for each module.
        return defaultHandler;
    }

}
```

- Example for group querying


```java
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
public final class GroupHistoryHandler extends BaseCustomizedContentHandler {

    @Override
    public List<Content> handleContent(CustomizedContent content, ReliableMessage rMsg,
                                       Messenger messenger) {
        if (content.getGroup() == null) {
            assert false : "group command error: " + content + ", sender: " + rMsg.getSender();
            return respondReceipt("Group command error.", rMsg.getEnvelope(), content, null);
        }
        String act = content.getAction();
        if (GroupHistory.ACT_QUERY.equals(act)) {
            //assert GroupHistory.APP.equals(content.getApplication());
            assert GroupHistory.MOD.equals(content.getModule());
            return transformQueryCommand(content, rMsg, messenger);
        }
        assert false : "unknown action: " + act + ", " + content + ", sender: " + rMsg.getSender();
        return super.handleContent(content, rMsg, messenger);
    }

    private List<Content> transformQueryCommand(CustomizedContent content, ReliableMessage rMsg,
                                                Messenger messenger) {
        Map<String, Object> info = content.copyMap(false);
        info.put("type", ContentType.COMMAND);
        info.put("command", QueryCommand.QUERY);
        Content query = Content.parse(info);
        if (query instanceof QueryCommand) {
            return messenger.processContent(query, rMsg);
        }
        assert false : "query command error: " + query + ", " + content + ", sender: " + rMsg.getSender();
        return respondReceipt("Query command error.", rMsg.getEnvelope(), content, null);
    }

}


//  void registerCustomizedHandlers() {
//      AppCustomizedFilter filter = new AppCustomizedFilter();
//      // 'chat.dim.group:history'
//      filter.setContentHandler(
//              GroupHistory.APP,
//              GroupHistory.MOD,
//              new GroupHistoryHandler()
//      );
//      SharedCustomizedFilter.customizedFilter = filter;
//  }
```

### ContentProcessorCreator

```java
import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.group.QueryCommand;


public class ClientContentProcessorCreator extends BaseContentProcessorCreator {

    public ClientContentProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public ContentProcessor createContentProcessor(String msgType) {
        switch (msgType) {

            // application customized
            case ContentType.APPLICATION:
            case ContentType.CUSTOMIZED:
                return new CustomizedContentProcessor(getFacebook(), getMessenger());
            
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

## Usage

To let your **AppCustomizedProcessor** start to work,
you must override ```BaseContentProcessorCreator``` for message types:

1. ContentType.APPLICATION 
2. ContentType.CUSTOMIZED

and then set your **creator** for ```GeneralContentProcessorFactory``` in the ```MessageProcessor```.

----

Copyright &copy; 2018-2026 Albert Moky
[![Followers](https://img.shields.io/github/followers/moky)](https://github.com/moky?tab=followers)