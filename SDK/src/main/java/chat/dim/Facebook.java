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

import chat.dim.core.Barrack;
import chat.dim.mkm.Entity;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.ID;


// -----------------------------------------------------------------------------
//  Facebook (Unified Entity Management)
// -----------------------------------------------------------------------------

/**
 *  Unified manager for user/group entity operations (combines caching + data access).
 *  <p>
 *      Implements core entity management workflows:
 *      1. Selects the correct local user for message decryption
 *      2. Retrieves/creates user/group entities (combines Barrack cache + lazy creation)
 *      3. Integrates with Archivist for persistent data access
 *  </p>
 *  <p>
 *      Implements: {@link Entity.Delegate}, {@link User.DataSource}, {@link Group.DataSource}
 *  </p>
 */
public abstract class Facebook implements Entity.Delegate, User.DataSource, Group.DataSource {

    /**
     *  Returns the entity cache manager (Barrack) - internal use only.
     *  <p>
     *      Null if the barrack is not initialized/ready for use.
     *  </p>
     *
     * @return barrack
     */
    protected abstract Barrack getBarrack();

    /**
     *  Selects a local user for decrypting messages to a user/broadcast receiver.
     *  <p>
     *      Core logic:
     *      0. Validates receiver type (only user/broadcast allowed)
     *      1. If receiver is broadcast &rarr; returns first local user (any user can decrypt)
     *      2. If receiver is user &rarr; returns matching local user (personal message target)
     *      3. Returns null if no matching local user is found
     *  </p>
     *
     * @param receiver is the target receiver ID (must be user or broadcast type).
     * @return a local user ID for decryption (null if no match).
     * @throws AssertionError if receiver is invalid (group) or local users are empty.
     */
    public ID selectUser(ID receiver) {
        assert receiver.isUser() || receiver.isBroadcast() : "user ID error: " + receiver;
        Barrack archivist = getBarrack();
        assert archivist != null : "archivist not ready";
        List<ID> allUsers = archivist.getLocalUsers();
        if (allUsers == null || allUsers.isEmpty()) {
            assert false : "local users should not be empty";
            return null;
        } else if (receiver.isBroadcast()) {
            // broadcast message can be decrypted by anyone, so
            // just return current user here
            return allUsers.get(0);
        }
        // personal message
        for (ID item : allUsers) {
            if (receiver.isSameAs(item)) {
                // DISCUSS: set this item to be current user?
                return item;
            }
        }
        // not for me?
        return null;
    }

    /**
     *  Selects a local user who is a member of a specific group (for group message decryption).
     *  <p>
     *      Core logic:
     *      0. Validates group member list is non-empty
     *      1. Finds the first local user that exists in the group member list
     *      2. Returns null if no local user is a group member
     *  </p>
     *
     * @param members is the list of group member IDs (must be non-empty).
     * @return a local user ID who is a group member (null if no match).
     * @throws AssertionError if members are empty or local users are empty.
     */
    public ID selectMember(List<ID> members) {
        assert members != null && !members.isEmpty() : "group members not found";
        Barrack archivist = getBarrack();
        assert archivist != null : "archivist not ready";
        List<ID> allUsers = archivist.getLocalUsers();
        if (allUsers == null || allUsers.isEmpty()) {
            assert false : "local users should not be empty";
            return null;
        }
        // group message (recipient not designated)
        for (ID item : allUsers) {
            for (ID did : members) {
                if (did.isSameAs(item)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        }
        // not for me?
        return null;
    }

    // -------------------------------------------------------------------------
    //  Entity Delegate Implementation (User Management)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    //  Entity Delegate Implementation (Group Management)
    // -------------------------------------------------------------------------

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
