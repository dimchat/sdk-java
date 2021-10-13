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

import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;

public class GroupCommandProcessor extends HistoryCommandProcessor {

    public static String FMT_GRP_CMD_NOT_SUPPORT = "Group command (name: %s) not support yet!";
    public static String STR_GROUP_EMPTY = "Group empty";

    public GroupCommandProcessor() {
        super();
    }

    protected List<ID> getMembers(final GroupCommand cmd) {
        // get from 'members'
        List<ID> members = cmd.getMembers();
        if (members == null) {
            members = new ArrayList<>();
            // get from 'member'
            final ID member = cmd.getMember();
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public List<Content> execute(final Command cmd, final ReliableMessage rMsg) {
        final String text = String.format(FMT_GRP_CMD_NOT_SUPPORT, cmd.getCommand());
        return respondText(text, cmd.getGroup());
    }

    @Override
    public List<Content> process(final Content content, final ReliableMessage rMsg) {
        assert content instanceof GroupCommand : "group command error: " + content;
        final Command cmd = (Command) content;
        // get CPU by command name
        CommandProcessor cpu = getProcessor(cmd);
        if (cpu == null) {
            cpu = this;
        } else {
            cpu.setMessenger(getMessenger());
        }
        return cpu.execute(cmd, rMsg);
    }
}
