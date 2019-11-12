package chat.dim.cpu.group;

import java.util.List;
import java.util.Locale;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.group.QuitCommand;

public class QuitCommandProcessor extends GroupCommandProcessor {

    public QuitCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private boolean doQuit(ID sender, ID group) {
        Facebook facebook = getFacebook();
        // existed members
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            throw new NullPointerException("Group members not found: " + group);
        }
        if (!members.contains(sender)) {
            return false;
        }
        members.remove(sender);
        if (facebook.saveMembers(members, group)) {
            return true;
        }
        return false;
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof QuitCommand;
        Facebook facebook = getFacebook();
        ID group = facebook.getID(content.getGroup());
        // 1. check permission
        if (facebook.isOwner(sender, group)) {
            String text = "owner cannot quit: " + sender + " -> " + group;
            throw new UnsupportedOperationException(text);
        }
        if (facebook.existsAssistant(sender, group)) {
            String text = "assistant cannot quit: " + sender + " -> " + group;
            throw new UnsupportedOperationException(text);
        }
        if (facebook.existsMember(sender, group)) {
            String text = "You are not a member of group: " + group;
            return new TextContent(text);
        }
        // 2. remove the sender from group members
        if (doQuit(sender, group)) {
            // failed to update group members
        }
        String text = String.format(Locale.CHINA, "Group command received: %s quit", sender);
        return new ReceiptCommand(text);
    }
}
