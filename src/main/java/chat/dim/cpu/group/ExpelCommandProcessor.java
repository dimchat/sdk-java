package chat.dim.cpu.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.group.ExpelCommand;

public class ExpelCommandProcessor extends GroupCommandProcessor {

    public ExpelCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private List<ID> doExpel(List<ID> expelList, ID group) {
        Facebook facebook = getFacebook();
        // existed members
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            throw new NullPointerException("Group members not found: " + group);
        }
        // removed list
        List<ID> removedList = new ArrayList<>();
        for (ID item: expelList) {
            if (!members.contains(item)) {
                continue;
            }
            // removing member found
            removedList.add(item);
            members.remove(item);
        }
        if (removedList.size() > 0) {
            if (facebook.saveMembers(members, group)) {
                return removedList;
            }
        }
        return null;
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof ExpelCommand;
        Facebook facebook = getFacebook();
        ID group = facebook.getID(content.getGroup());
        // 1. check permission
        if (!facebook.isOwner(sender, group)) {
            if (!facebook.existsAssistant(sender, group)) {
                String text = sender + " is not the owner/assistant of group " + group + ", cannot expel member.";
                throw new UnsupportedOperationException(text);
            }
        }
        // 2. get expelling members
        List<ID> expelList = getMembers((GroupCommand) content);
        if (expelList == null || expelList.size() == 0) {
            throw new NullPointerException("expel command error: " + content);
        }
        // 2.1. get removed-list
        List<ID> removed = doExpel(expelList, group);
        if (removed != null) {
            content.put("removed", removed);
        }
        // 3. response
        int count = removed == null ? 0 : removed.size();
        String text = String.format(Locale.CHINA, "Group command received: expelled %d member(s)", count);
        return new ReceiptCommand(text);
    }
}
