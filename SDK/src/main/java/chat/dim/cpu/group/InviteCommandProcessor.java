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
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.group.InviteCommand;

public class InviteCommandProcessor extends ResetCommandProcessor {

    public static String STR_INVITE_CMD_ERROR = "Invite command error.";
    public static String STR_INVITE_NOT_ALLOWED = "Sorry, you are not allowed to invite new members into this group.";

    public InviteCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof InviteCommand : "invite command error: " + content;
        GroupCommand command = (GroupCommand) content;
        Facebook facebook = getFacebook();

        // 0. check group
        ID group = command.getGroup();
        ID owner = facebook.getOwner(group);
        List<ID> members = facebook.getMembers(group);
        if (owner == null || members == null || members.size() == 0) {
            // NOTICE: group membership lost?
            //         reset group members
            return temporarySave(command, rMsg.getSender());
        }

        // 1. check permission
        ID sender = rMsg.getSender();
        if (!members.contains(sender)) {
            // not a member? check assistants
            List<ID> assistants = facebook.getAssistants(group);
            if (assistants == null || !assistants.contains(sender)) {
                return respondText(STR_INVITE_NOT_ALLOWED, group);
            }
        }

        // 2. inviting members
        List<ID> inviteList = getMembers(command);
        if (inviteList.size() == 0) {
            return respondText(STR_INVITE_CMD_ERROR, group);
        }
        // 2.1. check for reset
        if (sender.equals(owner) && inviteList.contains(owner)) {
            // NOTICE: owner invites owner?
            //         it means this should be a 'reset' command
            return temporarySave(command, rMsg.getSender());
        }
        // 2.2. build invited-list
        List<String> addedList = new ArrayList<>();
        for (ID item: inviteList) {
            if (members.contains(item)) {
                continue;
            }
            // new member found
            addedList.add(item.toString());
            members.add(item);
        }
        // 2.3. do invite
        if (addedList.size() > 0) {
            if (facebook.saveMembers(members, group)) {
                command.put("added", addedList);
            }
        }

        // 3. response (no need to response this group command)
        return null;
    }
}
