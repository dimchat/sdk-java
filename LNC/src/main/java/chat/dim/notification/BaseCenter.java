/* license: https://mit-license.org
 *
 *  LNC: Local Notification Center
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
package chat.dim.notification;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class BaseCenter {

    // name => WeakSet<Observer>
    private final Map<String, Set<Observer>> allObservers = new Hashtable<>();
    private final ReadWriteLock observerLock = new ReentrantReadWriteLock();

    public void addObserver(Observer observer, String name) {
        Lock writeLock = observerLock.writeLock();
        writeLock.lock();
        try {
            Set<Observer> listeners = allObservers.get(name);
            if (listeners == null) {
                // WeakSet<E>
                listeners = Collections.newSetFromMap(new WeakHashMap<>());
                listeners.add(observer);
                allObservers.put(name, listeners);
            } else {
                listeners.add(observer);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void removeObserver(Observer observer, String name) {
        Lock writeLock = observerLock.writeLock();
        writeLock.lock();
        try {
            Set<Observer> listeners = allObservers.get(name);
            if (listeners != null && listeners.remove(observer)) {
                // observer removed
                if (listeners.size() == 0) {
                    allObservers.remove(name);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void removeObserver(Observer observer) {
        Lock writeLock = observerLock.writeLock();
        writeLock.lock();
        try {
            Iterator<Map.Entry<String, Set<Observer>>> iterator = allObservers.entrySet().iterator();
            Map.Entry<String, Set<Observer>> entry;
            Set<Observer> listeners;
            while (iterator.hasNext()) {
                entry = iterator.next();
                listeners = entry.getValue();
                if (listeners != null && listeners.remove(observer)) {
                    // observer removed
                    if (listeners.size() == 0) {
                        iterator.remove();
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    protected Observer[] getObservers(String name) {
        Observer[] observers = null;
        Lock writeLock = observerLock.writeLock();
        writeLock.lock();
        try {
            Set<Observer> listeners = allObservers.get(name);
            if (listeners != null && listeners.size() > 0) {
                observers = new Observer[listeners.size()];
                observers = listeners.toArray(observers);
            }
        } finally {
            writeLock.unlock();
        }
        return observers;
    }

    public void postNotification(String name, Object sender) {
        postNotification(new Notification(name, sender, null));
    }

    public void postNotification(String name, Object sender, Map<String, Object> userInfo) {
        postNotification(new Notification(name, sender, userInfo));
    }

    public abstract void postNotification(Notification notification);
}
