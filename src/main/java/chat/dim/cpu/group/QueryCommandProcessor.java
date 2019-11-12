package chat.dim.cpu.group;

import java.util.List;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.ResetCommand;

public class QueryCommandProcessor extends GroupCommandProcessor {

    public QueryCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof QueryCommand;
        Facebook facebook = getFacebook();
        ID group = facebook.getID(content.getGroup());
        // 1. check permission
        if (!facebook.existsMember(sender, group)) {
            if (!facebook.existsAssistant(sender, group)) {
                String text = sender + " is not a member/assistant of group " + group + ", cannot query.";
                throw new UnsupportedOperationException(text);
            }
        }
        // 2. get members
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            String text = "Group members not found: " + group;
            throw new NullPointerException(text);
        }
        return new ResetCommand(group, members);
    }
}
