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
package chat.dim;

import java.util.List;

import chat.dim.core.Archivist;
import chat.dim.core.Barrack;
import chat.dim.mkm.Entity;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.ID;

public abstract class Facebook implements Entity.Delegate, User.DataSource, Group.DataSource {

    protected abstract Barrack getBarrack();

    public abstract Archivist getArchivist();

    /**
     *  Select local user for receiver
     *
     * @param receiver - user/broadcast ID
     * @return local user
     */
    public ID selectUser(ID receiver) {
        assert receiver.isUser() || receiver.isBroadcast() : "user ID error: " + receiver;
        Archivist archivist = getArchivist();
        assert archivist != null : "archivist not ready";
        List<ID> allUsers = archivist.getLocalUsers();
        //
        //  1.
        //
        if (allUsers == null || allUsers.isEmpty()) {
            assert false : "local users should not be empty";
            return null;
        } else if (receiver.isBroadcast()) {
            // broadcast message can be decrypted by anyone, so
            // just return current user here
            return allUsers.get(0);
        }
        //
        //  2. personal message
        //
        for (ID item : allUsers) {
            if (receiver.equals(item)) {
                // DISCUSS: set this item to be current user?
                return item;
            }
        }
        // not me?
        return null;
    }

    /**
     *  Select local user for group members
     *
     * @param members - group members
     * @return local user
     */
    public ID selectMember(List<ID> members) {
        assert members != null && !members.isEmpty() : "group members not found";
        Archivist archivist = getArchivist();
        assert archivist != null : "archivist not ready";
        List<ID> allUsers = archivist.getLocalUsers();
        //
        //  1.
        //
        if (allUsers == null || allUsers.isEmpty()) {
            assert false : "local users should not be empty";
            return null;
        }
        //
        //  2. group message (recipient not designated)
        //
        // the messenger will check group info before decrypting message,
        // so we can trust that the group's meta & members MUST exist here.
        for (ID item : allUsers) {
            if (members.contains(item)) {
                // DISCUSS: set this item to be current user?
                return item;
            }
        }
        // not me?
        return null;
    }

    //-------- Entity Delegate

    @Override
    public User getUser(ID uid) {
        assert uid.isUser() : "user ID error: " + uid;
        Barrack barrack = getBarrack();
        if (barrack == null) {
            assert false : "barrack not ready";
            return null;
        }
        // get from user cache
        User user = barrack.getUser(uid);
        if (user == null) {
            // create user and cache it
            user = barrack.createUser(uid);
            if (user != null) {
                barrack.cacheUser(user);
            }
        }
        return user;
    }

    @Override
    public Group getGroup(ID gid) {
        assert gid.isGroup() : "group ID error: " + gid;
        Barrack barrack = getBarrack();
        if (barrack == null) {
            assert false : "barrack not ready";
            return null;
        }
        // get from group cache
        Group group = barrack.getGroup(gid);
        if (group == null) {
            // create group and cache it
            group = barrack.createGroup(gid);
            if (group != null) {
                barrack.cacheGroup(group);
            }
        }
        return group;
    }

}
