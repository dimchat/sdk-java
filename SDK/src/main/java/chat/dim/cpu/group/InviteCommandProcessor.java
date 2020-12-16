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
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.group.InviteCommand;

public class InviteCommandProcessor extends GroupCommandProcessor {

    public InviteCommandProcessor() {
        super();
    }

    // check whether this is a Reset command
    private boolean isReset(List<ID> inviteList, ID sender, ID group) {
        Facebook facebook = getFacebook();
        // NOTICE: owner invite owner?
        //         it's a Reset command!
        if (containsOwner(inviteList, group)) {
            return facebook.isOwner(sender, group);
        }
        return false;
    }

    private Content callReset(Content content, ReliableMessage rMsg) {
        CommandProcessor cpu = getProcessor(GroupCommand.RESET);
        assert cpu != null : "reset CPU not register yet";
        return cpu.process(content, rMsg);
    }

    private List<String> doInvite(List<ID> inviteList, ID group) {
        Facebook facebook = getFacebook();
        // existed members
        List<ID> members = facebook.getMembers(group);
        if (members == null) {
            members = new ArrayList<>();
        }
        // added list
        List<String> addedList = new ArrayList<>();
        for (ID item: inviteList) {
            if (members.contains(item)) {
                continue;
            }
            // adding member found
            addedList.add(item.toString());
            members.add(item);
        }
        if (addedList.size() > 0) {
            if (facebook.saveMembers(members, group)) {
                return addedList;
            }
        }
        return null;
    }

    @Override
    public Content process(Content content, ReliableMessage rMsg) {
        assert content instanceof InviteCommand : "invite command error: " + content;
        ID group = content.getGroup();
        // 0. check whether group info empty
        if (isEmpty(group)) {
            // NOTICE:
            //     group membership lost?
            //     reset group members
            return callReset(content, rMsg);
        }
        // 1. check permission
        ID sender = rMsg.getSender();
        Facebook facebook = getFacebook();
        if (!facebook.containsMember(sender, group)) {
            if (!facebook.containsAssistant(sender, group)) {
                if (!facebook.isOwner(sender, group)) {
                    String text = sender + " is not a member/assistant of group " + group + ", cannot invite member.";
                    throw new UnsupportedOperationException(text);
                }
            }
        }
        // 2. get inviting members
        List<ID> inviteList = getMembers((GroupCommand) content);
        if (inviteList == null || inviteList.size() == 0) {
            throw new NullPointerException("invite command error: " + content);
        }
        // 2.1. check for reset
        if (isReset(inviteList, sender, group)) {
            // NOTICE: owner invites owner?
            //         it means this should be a 'reset' command
            return callReset(content, rMsg);
        }
        // 2.2. get invited-list
        List<String> added = doInvite(inviteList, group);
        if (added != null) {
            content.put("added", added);
        }
        // 3. response (no need to response this group command)
        return null;
    }
}
