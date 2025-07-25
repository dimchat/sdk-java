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

public class CacheHolder <V> {

    private V value;

    private final long lifeSpan;
    private long expired;     // time to expired
    private long deprecated;  // time to deprecated

    public CacheHolder(V cacheValue, long cacheLifeSpan, long now) {
        super();
        value = cacheValue;
        lifeSpan = cacheLifeSpan;
        if (now <= 0) {
            now = System.currentTimeMillis();
        }
        expired = now + lifeSpan;
        deprecated = now + lifeSpan << 1;
    }

    public V getValue() {
        return value;
    }

    public void update(V newValue, long now) {
        value = newValue;
        if (now <= 0) {
            now = System.currentTimeMillis();
        }
        expired = now + lifeSpan;
        deprecated = now + lifeSpan << 1;
    }

    public boolean isAlive(long now) {
        if (now <= 0) {
            now = System.currentTimeMillis();
        }
        return now < expired;
    }

    public boolean isDeprecated(long now) {
        if (now <= 0) {
            now = System.currentTimeMillis();
        }
        return now > deprecated;
    }

    public void renewal(long duration, long now) {
        if (duration <= 0) {
            duration = 128 * 1000;
        }
        if (now <= 0) {
            now = System.currentTimeMillis();
        }
        expired = now + duration;
        deprecated = now + lifeSpan << 1;
    }
}
