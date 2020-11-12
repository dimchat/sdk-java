
import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.Entity;
import chat.dim.Immortals;
import chat.dim.KeyCache;
import chat.dim.KeyStore;
import chat.dim.User;
import chat.dim.core.Barrack;
import chat.dim.core.Transceiver;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.TextContentProcessor;
import chat.dim.dkd.PlainMessage;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
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
    static Transceiver transceiver;

    static {
        ContentProcessor.register(ContentType.TEXT, TextContentProcessor.class);
        CommandProcessor.register(Command.HANDSHAKE, HandshakeCommandProcessor.class);

        barrack = MyFacebook.getInstance();

        // keystore
        try {
            Map keys = new HashMap();
            keyStore = KeyStore.getInstance();
            boolean changed = keyStore.updateKeys(keys);
            keyStore.flush();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            keyStore = null;
        }

        // transceiver
        transceiver = new Transceiver();
        transceiver.setEntityDelegate(barrack);
        transceiver.setCipherKeyDelegate(keyStore);
    }

    @Test
    public void testUser() {

        ID identifier = Entity.parseID(Immortals.MOKI);
        User user = barrack.getUser(identifier);
        Log.info("user: " + user);
    }

    @Test
    public void testGroupCommand() {
        ID groupID = Entity.parseID("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        JoinCommand join = new JoinCommand(groupID);
        Log.info("join: " + join);
        assertEquals(GroupCommand.JOIN, join.getCommand());
    }

    @Test
    public void testTransceiver() {

        ID sender = Entity.parseID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        ID receiver = Entity.parseID("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");

        Content content = new TextContent("Hello");

        InstantMessage iMsg = new PlainMessage(content, sender, receiver);
        iMsg.setDelegate(transceiver);
        SecureMessage sMsg = transceiver.encryptMessage(iMsg);
        ReliableMessage rMsg = transceiver.signMessage(sMsg);

        SecureMessage sMsg2 = transceiver.verifyMessage(rMsg);
        InstantMessage iMsg2 = transceiver.decryptMessage(sMsg2);

        Log.info("send message: " + iMsg2);
    }

    @Test
    public void testBarrack() {
        ID identifier = Entity.parseID("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");

        Meta meta = barrack.getMeta(identifier);
        Log.info("meta: " + meta);

        identifier = Entity.parseID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        User user = barrack.getUser(identifier);

//        identifier = ID.getInstance("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
//
//        Group group = barrack.getGroup(identifier);

        Map<ID, Meta> map = new HashMap<>();
        identifier = null;
        meta = map.get(identifier);
        Log.info("meta: " + meta);
    }
}
