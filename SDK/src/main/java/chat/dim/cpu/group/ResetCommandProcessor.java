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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.ID;
import chat.dim.Messenger;
import chat.dim.ReliableMessage;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.ResetCommand;

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
                    getMessenger().sendContent(cmd, owner, null, 1);
                }
            }
            // response (no need to response this group command)
            return null;
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
        Map<String, Object> result = new HashMap<>();
        if (addedList.size() > 0 || removedList.size() > 0) {
            if (!facebook.saveMembers(newMembers, group)) {
                // failed to update members
                return result;
            }
            if (addedList.size() > 0) {
                result.put("added", addedList);
            }
            if (removedList.size() > 0) {
                result.put("removed", removedList);
            }
        }
        return result;
    }

    @Override
    public Content process(Content content, ID sender, ReliableMessage<ID, SymmetricKey> rMsg) {
        assert content instanceof ResetCommand || content instanceof InviteCommand : "reset command error: " + content;
        ID group = content.getGroup();
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
        Facebook facebook = getFacebook();
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
        // 3. response (no need to response this group command)
        return null;
    }
}
