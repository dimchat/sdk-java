
import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.Immortals;
import chat.dim.KeyCache;
import chat.dim.KeyStore;
import chat.dim.MessageProcessor;
import chat.dim.Messenger;
import chat.dim.User;
import chat.dim.core.Barrack;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.group.JoinCommand;

public class Tests extends TestCase {

    static Barrack barrack;
    static KeyCache keyStore;
    static Messenger transceiver;
    static MessageProcessor processor;

    static {
        ContentProcessor.register(0, new AnyContentProcessor());
        CommandProcessor.register(Command.HANDSHAKE, new HandshakeCommandProcessor());

        barrack = MyFacebook.getInstance();

        // keystore
        Map keys = new HashMap();
        keyStore = KeyStore.getInstance();
        keyStore.updateKeys(keys);
        keyStore.flush();

        // transceiver
        transceiver = new Messenger() {
            @Override
            public boolean saveMessage(InstantMessage msg) {
                return false;
            }

            @Override
            public void suspendMessage(ReliableMessage msg) {

            }

            @Override
            public void suspendMessage(InstantMessage msg) {

            }
        };
        transceiver.setEntityDelegate(barrack);
        transceiver.setCipherKeyDelegate(keyStore);

        processor = new MessageProcessor(transceiver);
        transceiver.setMessageProcessor(processor);
    }

    @Test
    public void testUser() {

        ID identifier = ID.parse(Immortals.MOKI);
        User user = barrack.getUser(identifier);
        Log.info("user: " + user);
    }

    @Test
    public void testGroupCommand() {
        ID groupID = ID.parse("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        JoinCommand join = new JoinCommand(groupID);
        Log.info("join: " + join);
        assertEquals(GroupCommand.JOIN, join.getCommand());
    }

    @Test
    public void testTransceiver() {

        ID sender = ID.parse("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        ID receiver = ID.parse("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");

        Envelope env = Envelope.create(sender, receiver, null);

        Content content = new TextContent("Hello");

        InstantMessage iMsg = InstantMessage.create(env, content);
        iMsg.setDelegate(transceiver);
        SecureMessage sMsg = processor.encryptMessage(iMsg);
        ReliableMessage rMsg = processor.signMessage(sMsg);

        SecureMessage sMsg2 = processor.verifyMessage(rMsg);
        InstantMessage iMsg2 = processor.decryptMessage(sMsg2);

        Log.info("send message: " + iMsg2);
    }

    @Test
    public void testBarrack() {
        ID identifier = ID.parse("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        Meta meta = barrack.getMeta(identifier);
        Log.info("meta: " + meta);

        identifier = ID.parse("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        User user = barrack.getUser(identifier);
        Log.info("user: " + user);

//        identifier = Entity.parseID("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
//        Group group = barrack.getGroup(identifier);
//        Log.info("group: " + group);
    }
}
