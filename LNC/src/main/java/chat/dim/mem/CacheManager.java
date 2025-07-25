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

import chat.dim.log.Log;

public enum CacheManager implements Runnable {

    INSTANCE;

    public static CacheManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, CachePool> poolMap = new HashMap<>();

    // thread for cleaning caches
    private Thread thread;
    private boolean running;

    CacheManager() {
        thread = null;
        running = false;
    }

    public void start() {
        forceStop();
        running = true;
        Thread thr = new Thread(this);
        thr.setDaemon(true);
        thr.start();
        thread = thr;
    }

    private void forceStop() {
        running = false;
        Thread thr = thread;
        if (thr != null) {
            // waiting 2 seconds for stopping the thread
            thread = null;
            try {
                thr.join(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        forceStop();
    }

    @Override
    public void run() {
        idle(1024);
        long nextTime = 0;
        long now;
        while (running) {
            now = System.currentTimeMillis();
            if (now < nextTime) {
                idle(2048);
                continue;
            } else {
                nextTime = now + 300 * 1000;
            }
            try {
                int count = purge(now);
                Log.info("[MEM] purge " + count + " item(s) from cache pools");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.info("[MEM] stop: " + this);
    }

    public static void idle(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Purge all pools
     *
     * @param now - current time
     */
    private int purge(long now) {
        int count = 0;
        Set<String> names = poolMap.keySet();
        CachePool<?, ?> pool;
        for (String key : names) {
            pool = poolMap.get(key);
            if (pool != null) {
                count += pool.purge(now);
            }
        }
        return count;
    }

    /**
     *  Get pool with name
     *
     * @param name - pool name
     * @param <K>  - key type
     * @param <V>  - value type
     * @return CachePool
     */
    @SuppressWarnings("unchecked")
    public <K, V> CachePool<K, V> getPool(String name) {
        CachePool<K, V> pool = poolMap.get(name);
        if (pool == null) {
            pool = new CachePool<>();
            poolMap.put(name, pool);
        }
        return pool;
    }
}
