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
package chat.dim.stargate.wormhole;

import java.lang.ref.WeakReference;
import java.util.*;

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Header;
import chat.dim.mtp.protocol.Package;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.tcp.*;
import chat.dim.tlv.Data;

public class Hole extends Thread implements Star, ConnectionHandler {

    private boolean running = false;

    // connection
    private Connection connection;

    private WeakReference<StarDelegate> delegateRef;

    public Hole(StarDelegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    private StarDelegate getDelegate() {
        if (delegateRef == null) {
            return null;
        }
        return delegateRef.get();
    }

    //
    //  Connection
    //

    protected Connection createConnection(String host, int port) {
        Connection connection = new ClientConnection(host, port);
        connection.start();
        return connection;
    }

    /**
     *  Create a connection for client
     *
     * @param host - server host
     * @param port - server port
     * @return connection
     */
    public Connection connect(String host, int port) {
        if (connection != null) {
            if (connection.port == port && connection.host.equals(host)) {
                return connection;
            }
        }
        connection = createConnection(host, port);
        return connection;
    }

    public void disconnect() {
        if (connection == null) {
            return;
        }
        connection.close();
    }

    private byte[] receive() {
        // 1. check received data
        byte[] buffer = connection.received();
        if (buffer == null || buffer.length < 8) {
            // received nothing
            return null;
        }
        Data data = new Data(buffer);
        int dataLen = data.getLength();
        Header head = Header.parse(data);
        if (head == null) {
            // not a D-MTP package?
            if (dataLen < 20) {
                // wait for more data
                return null;
            }
            int pos = data.find(Header.MAGIC_CODE, 1);
            if (pos > 0) {
                // found next head(starts with 'DIM'), skip data before it
                connection.receive(pos);
            } else {
                // skip the whole data
                connection.receive(dataLen);
            }
            return null;
        }
        // 2. receive data with 'head.length + body.length'
        int headLen = head.getLength();
        int bodyLen = head.bodyLength;
        if (bodyLen < 0) {
            // should not happen
            bodyLen = dataLen - headLen;
        }
        int packLen = headLen + bodyLen;
        // receive package
        buffer = connection.receive(packLen);
        assert buffer.length == packLen : "failed to receive package: " + packLen + ", " + buffer.length;
        data = new Data(buffer);
        // 3. return body with remote address
        return data.getBytes(headLen);
    }

    private static void _sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        running = true;
        super.start();;
    }

    public void close() {
        running = false;
    }

    @Override
    public void run() {
        byte[] cargo;
        List<byte[]> responses;
        while (running) {
            cargo = receive();
            if (cargo == null) {
                // received nothing, have a rest ^_^
                _sleep(200);
            } else {
                // dispatch
                responses = dispatch(cargo);
                for (byte[] res : responses) {
                    send(res);
                }
            }

        }
    }

    private List<byte[]> dispatch(byte[] payload) {
        List<byte[]> responses = new ArrayList<>();
        // TODO: call the listeners
        return responses;
    }

    private void ping() {
        long now = (new Date()).getTime();
        if (connection.isExpired(now)) {
            connection.send(HEARTBEAT);
        }
    }

    private static final byte[] PING = {'P', 'I', 'N', 'G'};
    private static final byte[] HEARTBEAT = Package.create(DataType.Message, 4, new Data(PING)).getBytes();

    //
    //  Star
    //

    @Override
    public StarStatus getStatus() {
        long now = (new Date()).getTime();
        ConnectionStatus connectionStatus = connection.getStatus(now);
        StarStatus starStatus = StarStatus.Init;
        switch (connectionStatus) {
            case Default: {
                starStatus = StarStatus.Connecting;
                break;
            }
            case Connecting: {
                starStatus = StarStatus.Connecting;
                break;
            }
            case Connected: {
                starStatus = StarStatus.Connected;
                break;
            }
            case Expired: {
                starStatus = StarStatus.Connected;
                break;
            }
            case Maintaining: {
                starStatus = StarStatus.Connected;
                break;
            }
            case Error: {
                starStatus = StarStatus.Error;
                break;
            }
        }
        return starStatus;
    }

    @Override
    public void launch(Map<String, Object> options) {

    }

    @Override
    public void terminate() {

    }

    @Override
    public void enterBackground() {

    }

    @Override
    public void enterForeground() {

    }

    @Override
    public void send(byte[] payload) {
        // packing for D-MTP
        Data body = new Data(payload);
        Package pack = Package.create(DataType.Message, body.getLength(), body);
        assert connection != null : "not connect yet";
        connection.send(pack.getBytes());
    }

    @Override
    public void send(byte[] payload, StarDelegate completionHandler) {
        // TODO: task list
        send(payload);
    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStatusChanged(Connection connection, ConnectionStatus oldStatus, ConnectionStatus newStatus) {
    }

    @Override
    public void onConnectionReceivedData(Connection connection) {
    }

    @Override
    public void onConnectionOverflowed(Connection connection, byte[] ejected) {
    }
}
