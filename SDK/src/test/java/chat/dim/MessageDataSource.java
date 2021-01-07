
package chat.dim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;

public class MessageDataSource implements Messenger.DataSource {

    private static final MessageDataSource ourInstance = new MessageDataSource();
    public static MessageDataSource getInstance() { return ourInstance; }
    private MessageDataSource() {
        super();
    }

    private final Map<ID, List<ReliableMessage>> incomingMessages = new HashMap<>();
    private final Map<ID, List<InstantMessage>> outgoingMessages = new HashMap<>();

    @Override
    public boolean saveMessage(InstantMessage iMsg) {
        // TODO: save message into local storage
        return false;
    }

    @Override
    public void suspendMessage(ReliableMessage rMsg) {
        // save this message in a queue waiting sender's meta response
        ID waiting = (ID) rMsg.get("waiting");
        if (waiting == null) {
            waiting = rMsg.getGroup();
            if (waiting == null) {
                waiting = rMsg.getSender();
            }
        } else {
            rMsg.remove("waiting");
        }
        List<ReliableMessage> list = incomingMessages.get(waiting);
        if (list == null) {
            list = new ArrayList<>();
            incomingMessages.put(waiting, list);
        }
        list.add(rMsg);
    }

    @Override
    public void suspendMessage(InstantMessage iMsg) {
        // save this message in a queue waiting receiver's meta response
        ID waiting = (ID) iMsg.get("waiting");
        if (waiting == null) {
            waiting = iMsg.getGroup();
            if (waiting == null) {
                waiting = iMsg.getSender();
            }
        } else {
            iMsg.remove("waiting");
        }
        List<InstantMessage> list = outgoingMessages.get(waiting);
        if (list == null) {
            list = new ArrayList<>();
            outgoingMessages.put(waiting, list);
        }
        list.add(iMsg);
    }
}
