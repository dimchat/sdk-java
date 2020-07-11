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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Header;
import chat.dim.mtp.protocol.Package;
import chat.dim.mtp.protocol.TransactionID;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.tcp.*;
import chat.dim.tlv.Data;

public class Hole extends Thread implements Star, ConnectionHandler {

    private boolean running = false;

    // connection
    private Connection connection;

    private final WeakReference<StarDelegate> delegateRef;

    // tasks for sending out
    private final List<Task> waitingList = new ArrayList<>();
    private final ReentrantReadWriteLock waitingLock = new ReentrantReadWriteLock();

    // handlers for callback
    private final Map<TransactionID, WeakReference<StarDelegate>> handlers = new HashMap<>();

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

    private StarDelegate getHandler(TransactionID sn) {
        WeakReference<StarDelegate> ref = handlers.get(sn);
        if (ref == null) {
            return null;
        }
        return ref.get();
    }

    private void setHandler(TransactionID sn, StarDelegate delegate) {
        handlers.put(sn, new WeakReference<>(delegate));
    }

    private void removeHandler(TransactionID sn) {
        handlers.remove(sn);
    }

    private Task getTask() {
        Task task = null;
        Lock writeLock = waitingLock.writeLock();
        writeLock.lock();
        try {
            if (waitingList.size() > 0) {
                task = waitingList.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return task;
    }

    private void addTask(Task task) {
        Lock writeLock = waitingLock.writeLock();
        writeLock.lock();
        try {
            waitingList.add(task);
        } finally {
            writeLock.unlock();
        }
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

    private Package receive() {
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
        return new Package(data, head, data.slice(headLen));
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
        super.start();
    }

    public void close() {
        running = false;
    }

    @Override
    public void run() {
        StarDelegate delegate;
        Package income;
        Task outgo;
        long now = (new Date()).getTime();
        long expired = now + Connection.EXPIRES;

        while (running) {
            try {
                // 1. send on task
                outgo = getTask();
                if (outgo != null) {
                    connection.send(outgo.getRequestData());
                    delegate = outgo.getHandler();
                    if (delegate != null) {
                        // set handler for callback when received response
                        setHandler(outgo.getTransactionID(), delegate);
                        _sleep(100);
                        // callback for sent
                        delegate.onFinishSend(outgo.getPayload(), null, this);
                    }
                }
                // 2. receive one package
                income = receive();
                if (income != null) {
                    // dispatch received package
                    delegate = getHandler(income.head.sn);
                    if (delegate == null) {
                        delegate = getDelegate();
                    }
                    if (delegate != null) {
                        // callback for received data
                        delegate.onReceive(income.body.getBytes(), this);
                        // remove handler
                        removeHandler(income.head.sn);
                    }
                }
                // 3. check time for next heartbeat
                now = (new Date()).getTime();
                if (now > expired) {
                    if (connection.isExpired(now)) {
                        connection.send(HEARTBEAT);
                    }
                    // try heartbeat next 2 seconds
                    expired = now + 2000;
                }
                if (outgo == null && income == null) {
                    // idling
                    _sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        String host = (String) options.get("host");
        int port = (int) options.get("port");

        connect(host, port);
        start();
    }

    @Override
    public void terminate() {
        close();
    }

    @Override
    public void enterBackground() {

    }

    @Override
    public void enterForeground() {

    }

    @Override
    public void send(byte[] payload) {
        send(payload, getDelegate());
    }

    @Override
    public void send(byte[] payload, StarDelegate completionHandler) {
        // packing for D-MTP
        Data body = new Data(payload);
        Package pack = Package.create(DataType.Message, body.getLength(), body);
        Task task = new Task(pack, completionHandler);
        addTask(task);
    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStatusChanged(Connection connection, ConnectionStatus oldStatus, ConnectionStatus newStatus) {
        StarDelegate delegate = getDelegate();
        if (delegate != null) {
            delegate.onStatusChanged(getStatus(), this);
        }
    }

    @Override
    public void onConnectionReceivedData(Connection connection) {
    }

    @Override
    public void onConnectionOverflowed(Connection connection, byte[] ejected) {
    }
}
