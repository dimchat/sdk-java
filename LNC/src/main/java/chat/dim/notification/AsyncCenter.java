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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  Asynchronous Notification Center
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *  call for each observers in background thread
 */
public class AsyncCenter extends BaseCenter implements Runnable {

    private final List<Notification> notifications = new ArrayList<>();
    private final ReadWriteLock notificationLock = new ReentrantReadWriteLock();

    private boolean running = false;
    private Thread thread = null;

    @Override
    public void postNotification(Notification notification) {
        Lock writeLock = notificationLock.writeLock();
        writeLock.lock();
        try {
            notifications.add(notification);
        } finally {
            writeLock.unlock();
        }
    }

    private Notification nextNotification() {
        Notification next = null;
        Lock writeLock = notificationLock.writeLock();
        writeLock.lock();
        try {
            if (notifications.size() > 0) {
                next = notifications.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return next;
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
            thread = null;
            try {
                thr.join(1024);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        forceStop();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        while (isRunning()) {
            if (!process()) {
                idle();
            }
        }
    }

    protected void idle() {
        try {
            Thread.sleep(128);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected boolean process() {
        Notification notification = nextNotification();
        if (notification == null) {
            // nothing to do now,
            // return false to have a rest ^_^
            return false;
        }
        post(notification);
        return true;
    }
}
