/* license: https://mit-license.org
 *
 *  Star Gate: Network Connection Module
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
package chat.dim.stargate;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import chat.dim.net.BaseHub;
import chat.dim.net.Connection;
import chat.dim.net.ConnectionState;
import chat.dim.startrek.Docker;
import chat.dim.startrek.StarGate;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

public class TCPGate extends StarGate implements Connection.Delegate {

    public final Connection connection;

    public TCPGate(Connection conn) {
        super();
        connection = conn;
    }

    @Override
    protected Docker createDocker() {
        // TODO: override to customize Worker
        if (MTPDocker.check(this)) {
            return new MTPDocker(this);
        }
        return null;
    }

    @Override
    public boolean isRunning() {
        // 1. StarGate not stopped
        // 2. Connection not closed (or still have data unprocessed?)
        return super.isRunning() && connection.isOpen();
    }

    @Override
    public boolean isExpired() {
        ConnectionState state = connection.getState();
        return state != null && state.equals(ConnectionState.EXPIRED);
    }

    @Override
    public Status getStatus() {
        return getStatus(connection.getState());
    }

    //
    //  Connection Status -> Gate Status
    //
    public static Status getStatus(ConnectionState state) {
        String name = state == null ? ConnectionState.DEFAULT : state.name;
        switch (name) {
            case ConnectionState.CONNECTING:
                return Status.CONNECTING;
            case ConnectionState.CONNECTED:
            case ConnectionState.MAINTAINING:
            case ConnectionState.EXPIRED:
                return Status.CONNECTED;
            case ConnectionState.ERROR:
                return Status.ERROR;
            default:
                return Status.INIT;
        }
    }

    @Override
    public boolean send(byte[] pack) {
        if (!connection.isOpen() || !connection.isConnected()) {
            return false;
        }
        SocketAddress remote = connection.getRemoteAddress();
        ByteBuffer buffer = ByteBuffer.allocate(pack.length);
        buffer.put(pack);
        buffer.flip();
        try {
            return connection.send(buffer, remote) != -1;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private byte[] receiveData() {
        if (!connection.isOpen() || !connection.isConnected()) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(BaseHub.MSS);
        try {
            if (connection.receive(buffer) == null) {
                // received nothing
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = new byte[buffer.position()];
        buffer.flip();
        buffer.get(data);
        return data;
    }

    @Override
    public byte[] receive(int length, boolean remove) {
        ByteArray fragment = receive(length);
        if (fragment == null) {
            return null;
        } else if (fragment.getSize() > length) {
            if (remove) {
                // fragment[length:]
                chunks = fragment.slice(length);
            }
            // fragment[:length]
            fragment = fragment.slice(0, length);
        } else if (remove) {
            //assert fragment.length == length : "fragment length error";
            chunks = null;
        }
        return fragment.getBytes();
    }

    private ByteArray chunks = null;

    private ByteArray receive(int length) {
        int cached = 0;
        if (chunks != null) {
            cached = chunks.getSize();
        }
        byte[] data;
        while (cached < length) {
            // try to receive data from connection
            data = receiveData();
            if (data == null || data.length == 0) {
                break;
            }
            // append data
            if (chunks == null) {
                chunks = new Data(data);
            } else {
                chunks = chunks.concat(data);
            }
            cached += data.length;
        }
        return chunks;
    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStateChanging(Connection connection, ConnectionState oldState, ConnectionState newState) {
        Status s1 = getStatus(oldState);
        Status s2 = getStatus(newState);
        if (!s1.equals(s2)) {
            Delegate delegate = getDelegate();
            if (delegate != null) {
                delegate.onStatusChanged(this, s1, s2);
            }
        }
    }
}
