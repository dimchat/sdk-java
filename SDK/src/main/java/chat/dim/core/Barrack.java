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

import chat.dim.mkm.Entity;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.ID;
import chat.dim.utils.MemoryCache;
import chat.dim.utils.ThanosCache;

/**
 *  Entity Factory
 *  ~~~~~~~~~~~~~~
 *  Entity pool to manage User/Group instances
 */
public class Barrack implements Entity.Delegate {

    // memory caches
    protected final MemoryCache<ID, User>   userCache = createUserCache();
    protected final MemoryCache<ID, Group> groupCache = createGroupCache();

    protected MemoryCache<ID, User> createUserCache() {
        return new ThanosCache<>();
    }
    protected MemoryCache<ID, Group> createGroupCache() {
        return new ThanosCache<>();
    }

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return number of survivors
     */
    public int reduceMemory() {
        int cnt1 = userCache.reduceMemory();
        int cnt2 = groupCache.reduceMemory();
        return cnt1 + cnt2;
    }

    protected void cacheUser(User user) {
        userCache.put(user.getIdentifier(), user);
    }

    protected void cacheGroup(Group group) {
        groupCache.put(group.getIdentifier(), group);
    }

    //
    //  Entity Delegate
    //

    @Override
    public User getUser(ID identifier) {
        return userCache.get(identifier);
    }

    @Override
    public Group getGroup(ID identifier) {
        return groupCache.get(identifier);
    }

}
