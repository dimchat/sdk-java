package chat.dim.cpu.group;

import java.util.*;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.group.QueryCommand;

public class ResetCommandProcessor extends GroupCommandProcessor {

    public ResetCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content temporarySave(List<ID> newMembers, ID sender, ID group) {
        if (containsOwner(newMembers, group)) {
            // it's a full list, save it now
            Facebook facebook = getFacebook();
            if (facebook.saveMembers(newMembers, group)) {
                ID owner = facebook.getOwner(group);
                if (owner != null && !owner.equals(sender)) {
                    // NOTICE: to prevent counterfeit,
                    //         query the owner for newest member-list
                    QueryCommand cmd = new QueryCommand(group);
                    getMessenger().sendContent(cmd, owner);
                }
                String text = String.format(Locale.CHINA, "Group command received: reset %d members", newMembers.size());
                return new ReceiptCommand(text);
            } else {
                String text = "Group command received, reset member failed.";
                return new ReceiptCommand(text);
            }
        } else {
            // NOTICE: this is a partial member-list
            //         query the sender for full-list
            return new QueryCommand(group);
        }
    }

    private Map<String, Object> doReset(List<ID> newMembers, ID group) {
        Facebook facebook = getFacebook();
        // existed members
        List<ID> members = facebook.getMembers(group);
        if (members == null) {
            members = new ArrayList<>();
        }
        // removed list
        List<ID> removedList = new ArrayList<>();
        for (ID item : members) {
            if (newMembers.contains(item)) {
                continue;
            }
            // removing member found
            removedList.add(item);
        }
        // added list
        List<ID> addedList = new ArrayList<>();
        for (ID item : newMembers) {
            if (members.contains(item)) {
                continue;
            }
            // adding member found
            addedList.add(item);
        }
        String text;
        if (addedList.size() > 0 || removedList.size() > 0) {
            if (facebook.saveMembers(newMembers, group)) {
                text = String.format(Locale.CHINA, "Group command received: reset %d members.", newMembers.size());
            } else {
                text = "Group command received, reset member failed.";
            }
        } else {
            text = "Group command received: reset";
        }
        Map<String, Object> result = new HashMap<>();
        if (addedList.size() > 0) {
            result.put("added", addedList);
        }
        if (removedList.size() > 0) {
            result.put("removed", removedList);
        }
        return result;
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof GroupCommand;
        Facebook facebook = getFacebook();
        ID group = facebook.getID(content.getGroup());
        // new members
        List<ID> newMembers = getMembers((GroupCommand) content);
        if (newMembers == null || newMembers.size() == 0) {
            throw new NullPointerException("reset group command error: " + content);
        }
        // 0. check whether group info empty
        if (isEmpty(group)) {
            // FIXME: group info lost?
            // FIXME: how to avoid strangers impersonating group member?
            return temporarySave(newMembers, sender, group);
        }
        // 1. check permission
        if (!facebook.isOwner(sender, group)) {
            if (!facebook.existsAssistant(sender, group)) {
                String text = sender + " is not the owner/assistant of group " + group + ", cannot reset members.";
                throw new UnsupportedOperationException(text);
            }
        }
        // 2. reset
        Map<String, Object> result = doReset(newMembers, group);
        Object added = result.get("added");
        if (added != null) {
            content.put("added", added);
        }
        Object removed = result.get("removed");
        if (removed != null) {
            content.put("removed", removed);
        }
        String text = "Group command received: reset";
        return new ReceiptCommand(text);
    }
}
