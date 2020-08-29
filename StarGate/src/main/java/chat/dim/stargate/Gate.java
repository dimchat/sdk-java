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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Header;
import chat.dim.mtp.protocol.Package;
import chat.dim.mtp.protocol.TransactionID;
import chat.dim.sg.Star;
import chat.dim.sg.StarStatus;
import chat.dim.tcp.ClientConnection;
import chat.dim.tcp.Connection;
import chat.dim.tcp.ConnectionHandler;
import chat.dim.tcp.ConnectionStatus;
import chat.dim.tlv.Data;

public class Gate extends Thread implements Star<TransactionID, Package>, ConnectionHandler {

    private Connection connection = null;
    private boolean running = false;

    private final WeakReference<Ship.Delegate> delegateRef;
    private final Map<TransactionID, WeakReference<Ship.Delegate>> handlers = new HashMap<>();

    // tasks for sending out
    private final List<Ship> urgentList = new ArrayList<>();
    private final List<Ship> normalList = new ArrayList<>();
    private final List<Ship> slowerList = new ArrayList<>();
    private final ReentrantReadWriteLock shipLock = new ReentrantReadWriteLock();

    public Gate(Ship.Delegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    private Ship.Delegate getDelegate() {
        return delegateRef.get();
    }

    private Ship.Delegate getHandler(TransactionID sn) {
        WeakReference<Ship.Delegate> ref = handlers.get(sn);
        if (ref == null) {
            return null;
        }
        return ref.get();
    }
    private void setHandler(TransactionID sn, Ship.Delegate delegate) {
        handlers.put(sn, new WeakReference<>(delegate));
    }
    private void removeHandler(TransactionID sn) {
        handlers.remove(sn);
    }

    private void addTask(Ship task) {
        Lock writeLock = shipLock.writeLock();
        writeLock.lock();
        try {
            int priority = task.getPriority();
            if (priority == Passenger.URGENT) {
                urgentList.add(task);
            } else if (priority == Passenger.NORMAL) {
                normalList.add(task);
            } else if (priority == Passenger.SLOWER) {
                normalList.add(task);
            } else {
                throw new IndexOutOfBoundsException("priority error: " + priority);
            }
        } finally {
            writeLock.unlock();
        }
    }
    private Ship getTask() {
        Ship task = null;
        Lock writeLock = shipLock.writeLock();
        writeLock.lock();
        try {
            if (urgentList.size() > 0) {
                task = urgentList.remove(0);
            } else if (normalList.size() > 0) {
                task = normalList.remove(0);
            } else if (slowerList.size() > 0) {
                task = slowerList.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return task;
    }

    //
    //  Connection
    //

    protected Connection createConnection(String host, int port) {
        Connection conn = new ClientConnection(host, port);
        conn.start();
        return conn;
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
        if (packLen > dataLen) {
            // wait for more data
            return null;
        }
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
        Ship.Delegate delegate;
        Package income;
        Ship outgo;
        byte[] body;
        long now = (new Date()).getTime();
        long expired = now + Connection.EXPIRES;

        while (running) {
            try {
                // 1. send one task
                outgo = getTask();
                if (outgo != null) {
                    connection.send(outgo.getRequestData());
                    delegate = outgo.getDelegate();
                    if (delegate != null) {
                        // set handler for callback when received response
                        setHandler(outgo.getTransactionID(), delegate);
                        _sleep(100);
                        // callback for sent
                        delegate.onSent(this, outgo.getPackage(), null);
                    }
                }
                // 2. receive one package
                income = receive();
                if (income != null && income.body.getLength() > 0) {
                    body = income.body.getBytes();
                    // TODO: process commands
                    if (body.length > 5 || (!Arrays.equals(body, PING) &&
                            !Arrays.equals(body, PONG) &&
                            !Arrays.equals(body, AGAIN)&&
                            !Arrays.equals(body, OK))) {
                        // dispatch received package
                        delegate = getHandler(income.head.sn);
                        if (delegate == null) {
                            delegate = getDelegate();
                        }
                        if (delegate != null) {
                            // callback for received data
                            delegate.onReceived(this, income);
                            // remove handler
                            removeHandler(income.head.sn);
                        }
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
    private static final byte[] PONG = {'P', 'O', 'N', 'G'};
    private static final byte[] AGAIN = {'A', 'G', 'A', 'I', 'N'};
    private static final byte[] OK = {'O', 'K'};

    private static final byte[] HEARTBEAT = Package.create(DataType.Message, 4, new Data(PING)).getBytes();

    //
    //  Star
    //

    private StarStatus getStatus(ConnectionStatus status) {
        switch (status) {
            case Default: {
                return StarStatus.Connecting;
            }
            case Connecting: {
                return StarStatus.Connecting;
            }
            case Connected: {
                return StarStatus.Connected;
            }
            case Expired: {
                return StarStatus.Connected;
            }
            case Maintaining: {
                return StarStatus.Connected;
            }
            case Error: {
                return StarStatus.Error;
            }
        }
        return StarStatus.Init;
    }

    @Override
    public StarStatus getStatus() {
        long now = (new Date()).getTime();
        return getStatus(connection.getStatus(now));
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

    @SuppressWarnings("unchecked")
    @Override
    public void send(chat.dim.sg.Passenger passenger, chat.dim.sg.StarDelegate delegate) {
        addTask(createShip(passenger, delegate));
    }

    protected Ship createShip(chat.dim.sg.Passenger passenger, chat.dim.sg.StarDelegate delegate) {
        return new Ship((Passenger) passenger, (Ship.Delegate) delegate);
    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStatusChanged(Connection connection, ConnectionStatus oldStatus, ConnectionStatus newStatus) {
        Ship.Delegate delegate = getDelegate();
        if (delegate != null) {
            delegate.onStatusChanged(this, getStatus(oldStatus), getStatus(newStatus));
        }
    }

    @Override
    public void onConnectionReceivedData(Connection connection) {
    }

    @Override
    public void onConnectionOverflowed(Connection connection, byte[] ejected) {
    }
}
