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
import chat.dim.protocol.group.ExpelCommand;

public class ExpelCommandProcessor extends GroupCommandProcessor {

    public ExpelCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private List<String> doExpel(List<ID> expelList, ID group) {
        Facebook facebook = getFacebook();
        // existed members
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            throw new NullPointerException("Group members not found: " + group);
        }
        // removed list
        List<String> removedList = new ArrayList<>();
        for (ID item: expelList) {
            if (!members.contains(item)) {
                continue;
            }
            // removing member found
            removedList.add(item.toString());
            members.remove(item);
        }
        if (removedList.size() > 0) {
            if (facebook.saveMembers(members, group)) {
                return removedList;
            }
        }
        return null;
    }

    @Override
    public Content process(Content content, ReliableMessage rMsg) {
        assert content instanceof ExpelCommand : "expel command error: " + content;
        ID sender = rMsg.getSender();
        ID group = content.getGroup();
        // 1. check permission
        Facebook facebook = getFacebook();
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
        List<String> removed = doExpel(expelList, group);
        if (removed != null) {
            content.put("removed", removed);
        }
        // 3. response (no need to response this group command)
        return null;
    }
}
