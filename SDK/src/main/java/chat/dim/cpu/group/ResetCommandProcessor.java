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
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.ResetCommand;

public class ResetCommandProcessor extends GroupCommandProcessor {

    public ResetCommandProcessor() {
        super();
    }

    private Content temporarySave(final GroupCommand cmd, final ID sender) {
        final Facebook facebook = getFacebook();
        final Messenger messenger = getMessenger();
        final ID group = cmd.getGroup();
        final QueryCommand query = new QueryCommand(group);
        // check whether the owner contained in the new members
        final List<ID> newMembers = getMembers(cmd);
        for (ID item : newMembers) {
            if (facebook.isOwner(item, group)) {
                // it's a full list, save it now
                if (facebook.saveMembers(newMembers, group)) {
                    if (!item.equals(sender)) {
                        // NOTICE: to prevent counterfeit,
                        //         query the owner for newest member-list
                        messenger.sendContent(null, item, query, null, 1);
                    }
                }
                // response (no need to respond this group command)
                return null;
            }
        }
        // NOTICE: this is a partial member-list
        //         query the sender for full-list
        return query;
    }

    @Override
    public List<Content> execute(final Command cmd, final ReliableMessage rMsg) {
        assert cmd instanceof ResetCommand || cmd instanceof InviteCommand : "reset command error: " + cmd;
        final Facebook facebook = getFacebook();

        // 0. check group
        final ID group = cmd.getGroup();
        final ID owner = facebook.getOwner(group);
        final List<ID> members = facebook.getMembers(group);
        if (owner == null || members == null || members.size() == 0) {
            // FIXME: group info lost?
            // FIXME: how to avoid strangers impersonating group member?
            final Content res = temporarySave((GroupCommand) cmd, rMsg.getSender());
            if (res == null) {
                return null;
            }
            final List<Content> responses = new ArrayList<>();
            responses.add(res);
            return responses;
        }

        // 1. check permission
        final ID sender = rMsg.getSender();
        if (!owner.equals(sender)) {
            // not the owner? check assistants
            final List<ID> assistants = facebook.getAssistants(group);
            if (assistants == null || !assistants.contains(sender)) {
                String text = sender + " is not the owner/assistant of group " + group + ", cannot reset members.";
                throw new UnsupportedOperationException(text);
            }
        }

        // 2. resetting members
        final List<ID> newMembers = getMembers((GroupCommand) cmd);
        if (newMembers.size() == 0) {
            throw new NullPointerException("group command error: " + cmd);
        }
        // 2.1. check owner
        if (!newMembers.contains(owner)) {
            throw new UnsupportedOperationException("cannot expel owner(" + owner + ") of group: " + group);
        }
        // 2.2. build expelled-list
        final List<String> removedList = new ArrayList<>();
        for (ID item : members) {
            if (newMembers.contains(item)) {
                continue;
            }
            // removing member found
            removedList.add(item.toString());
        }
        // 2.3. build invited-list
        final List<String> addedList = new ArrayList<>();
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
                    cmd.put("added", addedList);
                }
                if (removedList.size() > 0) {
                    cmd.put("removed", removedList);
                }
            }
        }

        // 3. response (no need to response this group command)
        return null;
    }
}
