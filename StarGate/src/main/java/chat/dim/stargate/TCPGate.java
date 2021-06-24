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

import chat.dim.startrek.Docker;
import chat.dim.startrek.StarGate;
import chat.dim.tcp.Connection;
import chat.dim.tcp.ConnectionState;
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
        // 2. Connection not closed or still have data unprocessed
        return super.isRunning() && connection.isRunning(); // || connection.available() > 0);
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
        if (connection.isRunning()) {
            return connection.send(new Data(pack)) == pack.length;
        } else {
            return false;
        }
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
        int available;
        ByteArray data;
        while (cached < length) {
            // check available length from connection
            available = connection.available();
            if (available <= 0) {
                break;
            }
            // try to receive data from connection
            data = connection.receive(available);
            if (data == null || data.getSize() == 0) {
                break;
            }
            // append data
            if (chunks == null) {
                chunks = data;
            } else {
                chunks = chunks.concat(data);
            }
            cached += data.getSize();
        }
        return chunks;
    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStateChanged(Connection connection, ConnectionState oldState, ConnectionState newState) {
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
