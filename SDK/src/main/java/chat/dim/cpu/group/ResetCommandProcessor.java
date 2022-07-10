/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.cpu.group;

import java.util.ArrayList;
import java.util.List;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.ResetCommand;

public class ResetCommandProcessor extends GroupCommandProcessor {

    public static String STR_RESET_CMD_ERROR = "Reset command error.";
    public static String STR_RESET_NOT_ALLOWED = "Sorry, you are not allowed to reset this group.";

    public ResetCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    protected void queryOwner(ID owner, ID group) {
        // TODO: send QueryCommand to owner
    }

    protected List<Content> temporarySave(GroupCommand command, ID sender) {
        Facebook facebook = getFacebook();
        ID group = command.getGroup();
        // check whether the owner contained in the new members
        List<ID> newMembers = getMembers(command);
        if (newMembers.size() == 0) {
            return respondText(STR_RESET_CMD_ERROR, group);
        }
        for (ID item : newMembers) {
            if (facebook.getMeta(item) == null) {
                // TODO: waiting for member's meta?
                continue;
            } else if (!facebook.isOwner(item, group)) {
                // not owner, skip it
                continue;
            }
            // it's a full list, save it now
            if (facebook.saveMembers(newMembers, group)) {
                if (!item.equals(sender)) {
                    // NOTICE: to prevent counterfeit,
                    //         query the owner for newest member-list
                    queryOwner(item, group);
                }
            }
            // response (no need to respond this group command)
            return null;
        }
        // NOTICE: this is a partial member-list
        //         query the sender for full-list
        return respondContent(GroupCommand.query(group));
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof ResetCommand || content instanceof InviteCommand : "reset command error: " + content;
        GroupCommand command = (GroupCommand) content;
        Facebook facebook = getFacebook();

        // 0. check group
        ID group = command.getGroup();
        ID owner = facebook.getOwner(group);
        List<ID> members = facebook.getMembers(group);
        if (owner == null || members == null || members.size() == 0) {
            // FIXME: group info lost?
            // FIXME: how to avoid strangers impersonating group member?
            return temporarySave(command, rMsg.getSender());
        }

        // 1. check permission
        ID sender = rMsg.getSender();
        if (!owner.equals(sender)) {
            // not the owner? check assistants
            List<ID> assistants = facebook.getAssistants(group);
            if (assistants == null || !assistants.contains(sender)) {
                return respondText(STR_RESET_NOT_ALLOWED, group);
            }
        }

        // 2. resetting members
        List<ID> newMembers = getMembers(command);
        if (newMembers.size() == 0) {
            return respondText(STR_RESET_CMD_ERROR, group);
        }
        // 2.1. check owner
        if (!newMembers.contains(owner)) {
            return respondText(STR_RESET_CMD_ERROR, group);
        }
        // 2.2. build expelled-list
        List<String> removedList = new ArrayList<>();
        for (ID item : members) {
            if (newMembers.contains(item)) {
                continue;
            }
            // removing member found
            removedList.add(item.toString());
        }
        // 2.3. build invited-list
        List<String> addedList = new ArrayList<>();
        for (ID item : newMembers) {
            if (members.contains(item)) {
                continue;
            }
            // adding member found
            addedList.add(item.toString());
        }
        // 2.4. do reset
        if (addedList.size() > 0 || removedList.size() > 0) {
            if (facebook.saveMembers(newMembers, group)) {
                if (addedList.size() > 0) {
                    command.put("added", addedList);
                }
                if (removedList.size() > 0) {
                    command.put("removed", removedList);
                }
            }
        }

        // 3. response (no need to response this group command)
        return null;
    }
}
