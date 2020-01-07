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

import chat.dim.*;
import chat.dim.cpu.group.*;
import chat.dim.protocol.Command;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.TextContent;

public class GroupCommandProcessor extends HistoryCommandProcessor {

    public GroupCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    @SuppressWarnings("unchecked")
    protected List<ID> getMembers(GroupCommand cmd) {
        List members = cmd.getMembers();
        if (members == null) {
            Object member = cmd.getMember();
            if (member == null) {
                return null;
            }
            members = new ArrayList();
            members.add(member);
        }
        return convertMembers(members);
    }

    // convert String list to ID list
    private List<ID> convertMembers(List members) {
        Facebook facebook = getFacebook();
        List<ID> array = new ArrayList<>();
        ID identifier;
        for (Object item : members) {
            identifier = facebook.getID(item);
            if (identifier == null) {
                throw new NullPointerException("Member ID error: " + item);
            }
            array.add(identifier);
        }
        return array;
    }

    // check whether the list contains owner
    protected boolean containsOwner(List members, ID group) {
        Facebook facebook = getFacebook();
        ID identifier;
        for (Object item : members) {
            identifier = facebook.getID(item);
            if (facebook.isOwner(identifier, group)) {
                return true;
            }
        }
        return false;
    }

    // check whether the group info is empty(lost)
    protected boolean isEmpty(ID group) {
        Facebook facebook = getFacebook();
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            return true;
        }
        ID owner = facebook.getOwner(group);
        return owner == null;
    }

    @Override
    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert getClass() == GroupCommandProcessor.class : "error!"; // override me!
        assert content instanceof Command : "group command error: " + content;
        // process command content by name
        String command = ((Command) content).command;
        CommandProcessor cpu = getCPU(command);
        if (cpu == null) {
            String text = String.format("Group command (%s) not support yet!", command);
            Content res = new TextContent(text);
            res.setGroup(content.getGroup());
            return res;
        }
        assert cpu != this : "Dead cycle!";
        return cpu.process(content, sender, iMsg);
    }

    static {

        //
        //  Register all processors with command name
        //

        register(GroupCommand.INVITE, InviteCommandProcessor.class);
        register(GroupCommand.EXPEL, ExpelCommandProcessor.class);
        register(GroupCommand.QUIT, QuitCommandProcessor.class);
        register(GroupCommand.RESET, ResetCommandProcessor.class);

        register(GroupCommand.QUERY, QueryCommandProcessor.class);
    }
}
