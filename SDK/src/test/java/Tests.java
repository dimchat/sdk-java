
import junit.framework.TestCase;
import org.junit.Test;

import chat.dim.Facebook;
import chat.dim.Group;
import chat.dim.KeyStore;
import chat.dim.MessageDataSource;
import chat.dim.Messenger;
import chat.dim.User;
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

    static Facebook barrack;
    static Messenger transceiver;

    static {
        ContentProcessor.register(0, new AnyContentProcessor());
        CommandProcessor.register(Command.HANDSHAKE, new HandshakeCommandProcessor());

        barrack = MyFacebook.getInstance();

        // transceiver
        transceiver = new Messenger() {
            @Override
            protected Facebook createFacebook() {
                return barrack;
            }
        };
        transceiver.setCipherKeyDelegate(KeyStore.getInstance());
        transceiver.setDataSource(MessageDataSource.getInstance());
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
        SecureMessage sMsg = transceiver.encryptMessage(iMsg);
        if (sMsg == null) {
            Log.info("failed to encrypt message: " + iMsg);
            return;
        }
        ReliableMessage rMsg = transceiver.signMessage(sMsg);
        if (rMsg == null) {
            Log.info("failed to sing message: " + sMsg);
            return;
        }

        SecureMessage sMsg2 = transceiver.verifyMessage(rMsg);
        if (sMsg2 == null) {
            Log.info("failed to verify message: " + rMsg);
            return;
        }
        InstantMessage iMsg2 = transceiver.decryptMessage(sMsg2);
        if (iMsg2 == null) {
            Log.info("failed to decript message: " + sMsg2);
            return;
        }

        Log.info("send message: " + iMsg2);
    }

    @Test
    public void testBarrack() {
        ID identifier = ID.parse("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        Meta meta = transceiver.getFacebook().getMeta(identifier);
        Log.info("meta: " + meta);

        identifier = ID.parse("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        meta = transceiver.getFacebook().getMeta(identifier);
        if (meta != null) {
            User user = transceiver.getFacebook().getUser(identifier);
            Log.info("user: " + user);
        }

        identifier = ID.parse("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        meta = transceiver.getFacebook().getMeta(identifier);
        if (meta != null) {
            Group group = barrack.getGroup(identifier);
            Log.info("group: " + group);
        }
    }
}
