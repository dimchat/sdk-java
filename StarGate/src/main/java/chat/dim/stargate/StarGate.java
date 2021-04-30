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
import java.net.Socket;

import chat.dim.tcp.ActiveConnection;
import chat.dim.tcp.BaseConnection;
import chat.dim.tcp.Connection;

public class StarGate implements Gate, Connection.Delegate, Runnable {

    public final Connection connection;
    public final Dock dock;

    private Worker worker;
    private WeakReference<Delegate> delegateRef;

    public StarGate(Connection conn) {
        super();
        connection = conn;
        dock = new Dock();
        worker = null;
        delegateRef = null;
    }

    public StarGate(Socket connectedSocket) {
        this(new BaseConnection(connectedSocket));
    }
    public StarGate(String remoteHost, int remotePort) {
        this(new ActiveConnection(remoteHost, remotePort));
    }
    public StarGate(String remoteHost, int remotePort, Socket connectedSocket) {
        this(new ActiveConnection(remoteHost, remotePort, connectedSocket));
    }

    // override for customized worker
    public Worker getWorker() {
        if (worker == null) {
            if (MTPDocker.check(connection)) {
                worker = new MTPDocker(this);
            }
        }
        return worker;
    }

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

    //
    //  Star
    //

    @Override
    public Status getStatus() {
        return Gate.getStatus(connection.getStatus());
    }

    @Override
    public void run() {

    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStatusChanged(Connection connection, Connection.Status oldStatus, Connection.Status newStatus) {
        Delegate delegate = getDelegate();
        if (delegate != null) {
            delegate.onStatusChanged(this, Gate.getStatus(oldStatus), Gate.getStatus(newStatus));
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
