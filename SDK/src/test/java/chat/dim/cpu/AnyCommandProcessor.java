
package chat.dim.cpu;

import chat.dim.Messenger;
import chat.dim.protocol.Command;

public class AnyCommandProcessor extends CommandProcessor {

    public AnyCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
    protected CommandProcessor newCommandProcessor(String command) {
        if (Command.HANDSHAKE.equalsIgnoreCase(command)) {
            return new HandshakeCommandProcessor(getMessenger());
        }

        return super.newCommandProcessor(command);
    }
}
