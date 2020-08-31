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
import chat.dim.tcp.ClientConnection;
import chat.dim.tcp.Connection;
import chat.dim.tcp.ConnectionHandler;
import chat.dim.tcp.ConnectionStatus;
import chat.dim.tlv.Data;

public class StarGate extends Thread implements Star<StarShip>, ConnectionHandler {

    public interface Delegate extends chat.dim.sg.Delegate<Package, StarGate> {
    }

    // flow control
    public static int MAX_INCOMES_PER_OUTGO = 4;
    // milliseconds
    public static int INCOME_INTERVAL = 8;
    public static int OUTGO_INTERVAL = 32;
    public static int IDLE_INTERVAL = 256;

    private Connection connection = null;
    private boolean running = false;

    private final WeakReference<Delegate> delegateRef;
    private final Map<TransactionID, WeakReference<Delegate>> handlers = new HashMap<>();

    // tasks for sending out
    private final List<Integer> priorityList = new ArrayList<>();
    private final Map<Integer, List<StarShip>> shipTables = new HashMap<>();
    private final ReentrantReadWriteLock shipLock = new ReentrantReadWriteLock();

    public StarGate(Delegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    private Delegate getDelegate() {
        return delegateRef.get();
    }

    private Delegate getHandler(TransactionID sn) {
        WeakReference<Delegate> ref = handlers.get(sn);
        if (ref == null) {
            return null;
        }
        return ref.get();
    }
    private void setHandler(TransactionID sn, Delegate delegate) {
        handlers.put(sn, new WeakReference<>(delegate));
    }
    private void removeHandler(TransactionID sn) {
        handlers.remove(sn);
    }

    private void addTask(StarShip task) {
        Lock writeLock = shipLock.writeLock();
        writeLock.lock();
        try {
            int priority = task.priority;
            List<StarShip> table = shipTables.get(priority);
            if (table == null) {
                // create new table for this priority
                table = new ArrayList<>();
                shipTables.put(priority, table);
                // insert the priority in a sorted list
                int index = 0;
                for (; index < priorityList.size(); ++index) {
                    if (priority < priorityList.get(index)) {
                        // insert priority before the bigger one
                        break;
                    }
                }
                priorityList.add(index, priority);
            }
            // append to tail
            table.add(task);
        } finally {
            writeLock.unlock();
        }
    }
    private StarShip getTask() {
        StarShip task = null;
        Lock writeLock = shipLock.writeLock();
        writeLock.lock();
        try {
            List<StarShip> table;
            for (int priority : priorityList) {
                table = shipTables.get(priority);
                if (table == null || table.size() == 0) {
                    continue;
                }
                // pop from the head
                task = table.remove(0);
                break;
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
        long now = (new Date()).getTime();
        long expired = now + Connection.EXPIRES;

        int count = 0;
        while (running) {
            try {
                // process incoming packages / outgoing tasks
                if (MAX_INCOMES_PER_OUTGO > 0) {
                    // incoming priority
                    if (count < MAX_INCOMES_PER_OUTGO) {
                        if (processIncome()) {
                            ++count;
                            continue;
                        }
                    }
                    // keep a chance for outgoing packages
                    count = 0;
                    if (processOutgo()) {
                        continue;
                    }
                } else {
                    assert MAX_INCOMES_PER_OUTGO != 0 : "can not set MAX_INCOMES_PER_OUTGO to 0!";
                    // outgoing priority
                    if (count > MAX_INCOMES_PER_OUTGO) {
                        if (processOutgo()) {
                            --count;
                            continue;
                        }
                    }
                    // keep a chance for incoming packages
                    count = 0;
                    if (processIncome()) {
                        continue;
                    }
                }

                // check time for next heartbeat
                now = (new Date()).getTime();
                if (now > expired) {
                    if (connection.isExpired(now)) {
                        connection.send(HEARTBEAT);
                    }
                    // try heartbeat next 2 seconds
                    expired = now + 2000;
                }
                // idling
                assert IDLE_INTERVAL > 0 : "IDLE_INTERVAL error: " + IDLE_INTERVAL;
                _sleep(IDLE_INTERVAL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private boolean processIncome() {
        Package income = receive();
        if (income == null) {
            // no more package now
            return false;
        }
        byte[] body = income.body.getBytes();
        if (body.length < 6 && (
                Arrays.equals(body, PING) ||
                Arrays.equals(body, PONG) ||
                Arrays.equals(body, AGAIN) ||
                Arrays.equals(body, OK))) {
            // TODO: process commands
            return false;
        }

        Delegate delegate = getHandler(income.head.sn);
        if (delegate == null) {
            delegate = getDelegate();
        }
        if (delegate != null) {
            // dispatch received package
            delegate.onReceived(this, income);
            // remove handler
            removeHandler(income.head.sn);
        }
        // flow control
        if (INCOME_INTERVAL > 0) {
            _sleep(INCOME_INTERVAL);
        }
        return true;
    }
    private boolean processOutgo() {
        StarShip outgo = getTask();
        if (outgo == null) {
            // no more task now
            return false;
        }
        // send out request data
        Package pack = outgo.getPackage();
        int res = connection.send(pack.getBytes());

        Delegate delegate = outgo.getDelegate();
        if (delegate == null) {
            delegate = getDelegate();
        } else if (res == pack.getLength()) {
            // set handler for callback when received response
            setHandler(outgo.getTransactionID(), delegate);
        }
        if (delegate != null) {
            if (res == pack.getLength()) {
                // callback for sent success
                delegate.onSent(this, outgo.getPackage(), null);
            } else {
                // callback for sent failed
                delegate.onSent(this, outgo.getPackage(), new StarError(outgo));
            }
        }
        // flow control
        if (OUTGO_INTERVAL > 0) {
            _sleep(OUTGO_INTERVAL);
        }
        return true;
    }

    public class StarError extends Error {
        public final StarShip ship;
        StarError(StarShip s) {
            super();
            ship = s;
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

    private Status getStatus(ConnectionStatus status) {
        switch (status) {
            case Default: {
                return Status.Connecting;
            }
            case Connecting: {
                return Status.Connecting;
            }
            case Connected: {
                return Status.Connected;
            }
            case Expired: {
                return Status.Connected;
            }
            case Maintaining: {
                return Status.Connected;
            }
            case Error: {
                return Status.Error;
            }
        }
        return Status.Init;
    }

    @Override
    public Status getStatus() {
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

    @Override
    public void send(StarShip ship) {
        addTask(ship);
    }

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStatusChanged(Connection connection, ConnectionStatus oldStatus, ConnectionStatus newStatus) {
        Delegate delegate = getDelegate();
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
