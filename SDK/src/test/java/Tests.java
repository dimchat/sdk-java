
import chat.dim.ID;
import chat.dim.Immortals;
import chat.dim.User;
import junit.framework.TestCase;
import org.junit.Test;

import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;

import cpu.HandshakeCommandProcessor;
import cpu.TextContentProcessor;

public class Tests extends TestCase {

    @Test
    public void testUser() {

        Facebook facebook = Facebook.getInstance();
        ID identifier = facebook.getID(Immortals.MOKI);
        User user = facebook.getUser(identifier);
        Log.info("user: " + user);
    }

    static {
        ContentProcessor.register(ContentType.TEXT, TextContentProcessor.class);
        CommandProcessor.register(Command.HANDSHAKE, HandshakeCommandProcessor.class);
    }
}
