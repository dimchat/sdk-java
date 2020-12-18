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
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.group.ExpelCommand;

public class ExpelCommandProcessor extends GroupCommandProcessor {

    public ExpelCommandProcessor() {
        super();
    }

    @Override
    public Content execute(Command cmd, ReliableMessage rMsg) {
        assert cmd instanceof ExpelCommand : "expel command error: " + cmd;
        Facebook facebook = getFacebook();

        // 0. check group
        ID group = cmd.getGroup();
        ID owner = facebook.getOwner(group);
        List<ID> members = facebook.getMembers(group);
        if (owner == null || members == null || members.size() == 0) {
            throw new NullPointerException("Group not ready: " + group);
        }

        // 1. check permission
        ID sender = rMsg.getSender();
        if (!owner.equals(sender)) {
            // not the owner? check assistants
            List<ID> assistants = facebook.getAssistants(group);
            if (assistants == null || !assistants.contains(sender)) {
                String text = sender + " is not the owner/assistant of group " + group + ", cannot expel member.";
                throw new UnsupportedOperationException(text);
            }
        }

        // 2. expelling members
        List<ID> expelList = getMembers((GroupCommand) cmd);
        if (expelList == null || expelList.size() == 0) {
            throw new NullPointerException("expel command error: " + cmd);
        }
        // 2.1. check owner
        if (expelList.contains(owner)) {
            throw new UnsupportedOperationException("cannot expel owner(" + owner + ") of group: " + group);
        }
        // 2.2. build expelled-list
        List<String> removedList = new ArrayList<>();
        for (ID item: expelList) {
            if (!members.contains(item)) {
                continue;
            }
            // removing member found
            removedList.add(item.toString());
            members.remove(item);
        }
        // 2.3. do expel
        if (removedList.size() > 0) {
            if (facebook.saveMembers(members, group)) {
                cmd.put("removed", removedList);
            }
        }

        // 3. response (no need to response this group command)
        return null;
    }
}
