/* license: https://mit-license.org
 *
 *  Star Gate: Interfaces for network connection
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.stargate;

import java.lang.ref.WeakReference;

import chat.dim.tcp.BaseConnection;
import chat.dim.tcp.Connection;

public class StarGate implements Gate, Connection.Delegate, Runnable {

    public final Connection connection;
    public final Dock dock = new Dock();

    private Worker worker = null;
    private WeakReference<Delegate> delegateRef = null;

    private boolean running = false;

    public StarGate(Connection conn) {
        super();
        connection = conn;
    }

    @Override
    public Dock getDock() {
        return dock;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public Worker getWorker() {
        if (worker == null) {
            worker = createWorker();
        }
        return worker;
    }

    // override to customize Worker
    protected Worker createWorker() {
        if (MTPDocker.check(connection)) {
            return new MTPDocker(this);
        } else {
            return null;
        }
    }

    public void setWorker(Worker docker) {
        worker = docker;
    }

    @Override
    public Delegate getDelegate() {
        if (delegateRef == null) {
            return null;
        } else {
            return delegateRef.get();
        }
    }
    public void setDelegate(Delegate delegate) {
        if (delegate == null) {
            delegateRef = null;
        } else {
            delegateRef = new WeakReference<>(delegate);
        }
    }

    @Override
    public Status getStatus() {
        return Gate.getStatus(connection.getStatus());
    }

    @Override
    public boolean send(byte[] payload, int priority, Ship.Delegate delegate) {
        Worker worker = getWorker();
        if (worker == null) {
            return false;
        } else if (getStatus().equals(Status.Connected)) {
            return worker.send(payload, priority, delegate);
        } else {
            return false;
        }
    }

    //
    //  Running
    //

    @Override
    public void run() {
        setup();
        try {
            handle();
        } finally {
            finish();
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        if (running) {
            if (connection instanceof BaseConnection) {
                BaseConnection bc = (BaseConnection) connection;
                // connection not closed, or more data to be processed
                return bc.isRunning() || bc.received() != null;
            } else {
                return true;
            }
        }
        return false;
    }

    public void setup() {
        running = true;
        // check worker
        while (getWorker() == null && isRunning()) {
            // waiting for worker
            idle();
        }
        // setup worker
        if (worker != null) {
            worker.setup();
        }
    }

    public void finish() {
        // clean worker
        if (worker != null) {
            worker.finish();
        }
    }

    public void handle() {
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

    public boolean process() {
        if (worker != null) {
            return worker.process();
        }
        return false;
    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStatusChanged(Connection connection, Connection.Status oldStatus, Connection.Status newStatus) {
        Delegate delegate = getDelegate();
        if (delegate != null) {
            Status s1 = Gate.getStatus(oldStatus);
            Status s2 = Gate.getStatus(newStatus);
            if (!s1.equals(s2)) {
                delegate.onStatusChanged(this, s1, s2);
            }
        }
    }

    @Override
    public void onConnectionReceivedData(Connection connection, byte[] data) {
        // received data will be processed in run loop,
        // do nothing here
    }

    @Override
    public void onConnectionOverflowed(Connection connection, byte[] ejected) {
        // TODO: connection cache pool is full,
        //       some received data will be ejected to here,
        //       the application should try to process them.
    }
}
