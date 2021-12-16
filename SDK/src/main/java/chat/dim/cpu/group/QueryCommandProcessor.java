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

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.User;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.ResetCommand;

public class QueryCommandProcessor extends GroupCommandProcessor {

    public static String STR_QUERY_NOT_ALLOWED = "Sorry, you are not allowed to query this group.";

    public QueryCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
    public List<Content> execute(Command cmd, ReliableMessage rMsg) {
        assert cmd instanceof QueryCommand : "query command error: " + cmd;
        Facebook facebook = getFacebook();

        // 0. check group
        ID group = cmd.getGroup();
        ID owner = facebook.getOwner(group);
        List<ID> members = facebook.getMembers(group);
        if (owner == null || members == null || members.size() == 0) {
            return respondText(STR_GROUP_EMPTY, group);
        }

        // 1. check permission
        ID sender = rMsg.getSender();
        if (!members.contains(sender)) {
            // not a member? check assistants
            List<ID> assistants = facebook.getAssistants(group);
            if (assistants == null || !assistants.contains(sender)) {
                return respondText(STR_QUERY_NOT_ALLOWED, group);
            }
        }

        // 2. respond
        User user = facebook.getCurrentUser();
        assert user != null : "current user not set yet";
        Content res;
        if (user.identifier.equals(owner)) {
            res = new ResetCommand(group, members);
        } else {
            res = new InviteCommand(group, members);
        }
        return respondContent(res);
    }
}
