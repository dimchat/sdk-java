/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim.core;

import chat.dim.mkm.BaseGroup;
import chat.dim.mkm.BaseUser;
import chat.dim.mkm.Bot;
import chat.dim.mkm.Group;
import chat.dim.mkm.ServiceProvider;
import chat.dim.mkm.Station;
import chat.dim.mkm.User;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;

/**
 *  Entity Factory
 *  <p>
 *      Entity pool to manage User/Group instances
 *  </p>
 */
public abstract class Barrack {

    public abstract void cacheUser(User user);

    public abstract void cacheGroup(Group group);

    public abstract User getUser(ID uid);

    public abstract Group getGroup(ID gid);

    /**
     *  Create user when visa.key exists
     *
     * @param uid - user ID
     * @return user, null on not ready
     */
    public User createUser(ID uid) {
        assert uid.isUser() : "user ID error: " + uid;
        int network = uid.getType();
        // check user type
        if (EntityType.STATION.equals(network)) {
            return new Station(uid);
        } else if (EntityType.BOT.equals(network)) {
            return new Bot(uid);
        }
        // general user, or 'anyone@anywhere'
        return new BaseUser(uid);
    }

    /**
     *  Create group when members exist
     *
     * @param gid - group ID
     * @return group, null on not ready
     */
    public Group createGroup(ID gid) {
        assert gid.isGroup() : "group ID error: " + gid;
        int network = gid.getType();
        // check group type
        if (EntityType.ISP.equals(network)) {
            return new ServiceProvider(gid);
        }
        // general group, or 'everyone@everywhere'
        return new BaseGroup(gid);
    }

}
