/* license: https://mit-license.org
 *
 *  LNC: Log, Notification & Cache
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim.mem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CachePool <K, V> {

    private final Map<K, CacheHolder<V>> holderMap = new HashMap<>();

    public Set<K> getKeys() {
        return holderMap.keySet();
    }

    public CacheHolder<V> update(K key, V value, long lifeSpan, long now) {
        return update(key, new CacheHolder<>(value, lifeSpan, now));
    }
    public CacheHolder<V> update(K key, CacheHolder<V> holder) {
        holderMap.put(key, holder);
        return holder;
    }

    public CachePair<V> erase(K key, long now) {
        CachePair<V> old = null;
        if (now > 0) {
            // get exists value before erasing
            old = fetch(key, now);
        }
        holderMap.remove(key);
        return old;
    }

    public CachePair<V> fetch(K key, long now) {
        CacheHolder<V> holder = holderMap.get(key);
        if (holder == null) {
            // holder not found
            return null;
        } else if (holder.isAlive(now)) {
            return new CachePair<>(holder.getValue(), holder);
        } else {
            // holder expired
            return new CachePair<>(null, holder);
        }
    }

    public int purge(long now) {
        if (now <= 0) {
            now = System.currentTimeMillis();
        }
        int count = 0;
        Set<K> allKeys = getKeys();
        CacheHolder<V> holder;
        for (K key : allKeys) {
            holder = holderMap.get(key);
            if (holder == null || holder.isDeprecated(now)) {
                // remove expired holders
                holderMap.remove(key);
                ++count;
            }
        }
        return count;
    }
}
