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
package chat.dim.cpu;

import java.util.ArrayList;
import java.util.List;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;

public class GroupCommandProcessor extends HistoryCommandProcessor {

    public static String FMT_GRP_CMD_NOT_SUPPORT = "Group command (name: %s) not support yet!";
    public static String STR_GROUP_EMPTY = "Group empty.";

    public GroupCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    protected List<ID> getMembers(GroupCommand command) {
        // get from 'members'
        List<ID> members = command.getMembers();
        if (members == null) {
            members = new ArrayList<>();
            // get from 'member'
            ID member = command.getMember();
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof GroupCommand : "group command error: " + content;
        GroupCommand command = (GroupCommand) content;
        String text = String.format(FMT_GRP_CMD_NOT_SUPPORT, command.getCmd());
        return respondText(text, command.getGroup());
    }
}
