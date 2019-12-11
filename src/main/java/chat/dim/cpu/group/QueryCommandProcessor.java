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

import chat.dim.*;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.ResetCommand;

public class QueryCommandProcessor extends GroupCommandProcessor {

    public QueryCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
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
            String text = String.format("Sorry, members not found in group: %s", group);
            Content res = new TextContent(text);
            res.setGroup(group);
            return res;
        }
        // 3. respond
        User user = facebook.getCurrentUser();
        assert user != null;
        if (facebook.isOwner(user.identifier, group)) {
            return new ResetCommand(group, members);
        } else {
            return new InviteCommand(group, members);
        }
    }
}
