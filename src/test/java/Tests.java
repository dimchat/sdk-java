
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
    public void testContent() {

    }

    static {
        ContentProcessor.register(ContentType.TEXT.value, TextContentProcessor.class);
        CommandProcessor.register(Command.HANDSHAKE, HandshakeCommandProcessor.class);
    }
}
