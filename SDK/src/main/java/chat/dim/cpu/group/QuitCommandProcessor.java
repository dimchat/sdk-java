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

import java.util.List;

import chat.dim.Content;
import chat.dim.Facebook;
import chat.dim.ID;
import chat.dim.Messenger;
import chat.dim.ReliableMessage;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.group.QuitCommand;

public class QuitCommandProcessor extends GroupCommandProcessor {

    public QuitCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private void doQuit(ID sender, ID group) {
        Facebook facebook = getFacebook();
        // existed members
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            throw new NullPointerException("Group members not found: " + group);
        }
        if (!members.contains(sender)) {
            return;
        }
        members.remove(sender);
        facebook.saveMembers(members, group);
    }

    @Override
    public Content<ID> process(Content<ID> content, ID sender, ReliableMessage<ID, SymmetricKey> rMsg) {
        assert content instanceof QuitCommand : "quit command error: " + content;
        ID group = content.getGroup();
        // 1. check permission
        Facebook facebook = getFacebook();
        if (facebook.isOwner(sender, group)) {
            String text = "owner cannot quit: " + sender + " -> " + group;
            throw new UnsupportedOperationException(text);
        }
        if (facebook.existsAssistant(sender, group)) {
            String text = "assistant cannot quit: " + sender + " -> " + group;
            throw new UnsupportedOperationException(text);
        }
        // 2. remove the sender from group members
        doQuit(sender, group);
        // 3. response (no need to response this group command)
        return null;
    }
}
