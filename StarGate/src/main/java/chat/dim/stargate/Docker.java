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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Header;
import chat.dim.mtp.protocol.Package;
import chat.dim.mtp.protocol.TransactionID;
import chat.dim.tcp.ClientConnection;
import chat.dim.tcp.Connection;
import chat.dim.tlv.Data;

public class Docker implements Worker {

    // flow control
    public static int MAX_INCOMES_PER_OUTGO = 4;
    // milliseconds
    public static int INCOME_INTERVAL = 8;
    public static int OUTGO_INTERVAL = 32;
    public static int IDLE_INTERVAL = 256;

    private final WeakReference<StarGate.Delegate> delegateRef;

    private final Set<TransactionID> waitingSet = new HashSet<>();
    private final Map<TransactionID, StarShip> waitingTable = new HashMap<>();

    private final Dock dock = new Dock();

    private Connection connection = null;

    public Docker(StarGate.Delegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    @Override
    public StarGate.Delegate getDelegate() {
        return delegateRef.get();
    }

    private void pushWaiting(TransactionID sn, StarShip ship) {
        waitingSet.add(sn);
        waitingTable.put(sn, ship.update());
    }
    private StarShip popWaiting(TransactionID sn) {
        waitingSet.remove(sn);
        return waitingTable.remove(sn);
    }
    @SuppressWarnings("unchecked")
    private StarShip popWaiting() {
        long expires = (new Date()).getTime() - StarShip.EXPIRES;
        StarShip ship;
        HashSet<TransactionID> waiting = (HashSet<TransactionID>)waitingSet;
        Set<TransactionID> set = (Set<TransactionID>)waiting.clone();
        for (TransactionID sn : set) {
            ship = waitingTable.get(sn);
            if (ship == null) {
                waitingSet.remove(sn);  // should not happen
                continue;
            }
            if (ship.timestamp > expires) {
                // not expired yet
                continue;
            }
            if (ship.retries < StarShip.RETRIES) {
                // dequeue this ship
                popWaiting(sn);
                // update timestamp and retries
                return ship.update();
            }
            // retried too many times
            if (ship.timestamp < (expires - StarShip.EXPIRES * StarShip.RETRIES * 4L)) {
                // remove timeout task
                popWaiting(sn);
            }
        }
        return null;
    }

    @Override
    public Connection connect(String host, int port) {
        if (connection != null) {
            if (connection.port == port && connection.host.equals(host)) {
                return connection;
            }
            disconnect();
        }
        connection = getConnection(host, port);
        return connection;
    }
    protected Connection getConnection(String host, int port) {
        // override for customized connection
        Connection connection = new ClientConnection(host, port);
        connection.start();
        return connection;
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public StarGate.Status getStatus() {
        long now = (new Date()).getTime();
        return Worker.getStatus(connection.getStatus(now));
    }

    @Override
    public void addTask(StarShip ship) {
        dock.addShip(ship);
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

    @Override
    public int process(StarGate star, int count) {
        // process incoming packages / outgoing tasks
        if (MAX_INCOMES_PER_OUTGO > 0) {
            // incoming priority
            if (count < MAX_INCOMES_PER_OUTGO) {
                if (processIncome(star)) {
                    return ++count;
                }
            }
            // keep a chance for outgoing packages
            if (processOutgo(star)) {
                return 0;
            }
        } else {
            assert MAX_INCOMES_PER_OUTGO != 0 : "can not set MAX_INCOMES_PER_OUTGO to 0!";
            // outgoing priority
            if (count > MAX_INCOMES_PER_OUTGO) {
                if (processOutgo(star)) {
                    return --count;
                }
            }
            // keep a chance for incoming packages
            if (processIncome(star)) {
                return 0;
            }
        }
        processHeartbeat();
        return 0;
    }

    private boolean processIncome(StarGate star) {
        Package income = receive();
        if (income == null) {
            // no more package now
            return false;
        }
        Header head = income.head;
        byte[] body = income.body.getBytes();

        // check data type
        DataType type = head.type;
        if (type.equals(DataType.Command)) {
            // respond for Command
            if (body.length == PING.length && Arrays.equals(body, PING)) {
                // 'PING' -> 'PONG'
                Package res = Package.create(DataType.CommandRespond, income.head.sn, PONG.length, new Data(PONG));
                StarShip ship = new StarShip(StarShip.SLOWER, res, null);
                addTask(ship);
            }
            return true;
        } else if (type.equals(DataType.CommandRespond)) {
            // process Command Respond
            /*
            byte[] body = income.body.getBytes();
            if (body.length == PONG.length && Arrays.equals(body, PONG)) {
                // just ignore
                return true;
            } else if (body.length == OK.length && Arrays.equals(body, OK)) {
                // just ignore
                return true;
            }
             */
            return true;
        } else if (type.equals(DataType.MessageFragment)) {
            // respond for Message Fragment
            /*
            Package res = Package.create(DataType.MessageRespond, head.sn, head.pages, head.offset, OK.length, new Data(OK));
            StarShip ship = new StarShip(StarShip.NORMAL, res, null);
            addTask(ship);
            // TODO: assemble for MessageFragment
             */
            return true;
        } else if (type.equals(DataType.Message)) {
            // respond for Message
            Package res = Package.create(DataType.MessageRespond, head.sn, OK.length, new Data(OK));
            StarShip ship = new StarShip(StarShip.NORMAL, res, null);
            addTask(ship);
        } else {
            assert type.equals(DataType.MessageRespond) : "data type error: " + type;
            // process Message Respond
            StarShip ship = popWaiting(head.sn);
            if (ship != null) {
                StarGate.Delegate delegate = ship.getDelegate();
                if (delegate != null) {
                    if (body.length == AGAIN.length && Arrays.equals(body, AGAIN)) {
                        delegate.onSent(star, ship.getPackage(), new StarGate.Error(ship,"Send the message again"));
                    } else {
                        delegate.onSent(star, ship.getPackage(), null);
                    }
                }
            }
            if (body.length == OK.length && Arrays.equals(body, OK)) {
                // just ignore
                return true;
            } else if (body.length == AGAIN.length && Arrays.equals(body, AGAIN)) {
                // TODO: mission failed, send the message again
                return true;
            }
        }

        if (body.length > 0) {
            StarGate.Delegate delegate = getDelegate();
            if (delegate != null) {
                // dispatch received package
                delegate.onReceived(star, income);
            }
        }

        // flow control
        if (INCOME_INTERVAL > 0) {
            _sleep(INCOME_INTERVAL);
        }
        return true;
    }

    private boolean processOutgo(StarGate star) {
        StarShip ship = dock.getShip();
        if (ship == null) {
            // no more task now
            ship = popWaiting();
            if (ship == null) {
                // no task expired now
                return false;
            }
        }
        Package outgo = ship.getPackage();
        Header head = outgo.head;

        // check data type
        DataType type = head.type;
        if (type.equals(DataType.Message)) {
            // set for callback when received response
            pushWaiting(ship.getTransactionID(), ship);
        }

        // send out request data
        int res = connection.send(outgo.getBytes());
        if (res != outgo.getLength()) {
            // callback for sent failed
            StarGate.Delegate delegate = ship.getDelegate();
            if (delegate != null) {
                delegate.onSent(star, outgo, new StarGate.Error(ship, "Socket error"));
            }
        }

        // flow control
        if (OUTGO_INTERVAL > 0) {
            _sleep(OUTGO_INTERVAL);
        }
        return true;
    }

    private void processHeartbeat() {
        // check time for next heartbeat
        long now = (new Date()).getTime();
        if (now > expired) {
            if (connection.isExpired(now)) {
                Package ping = Package.create(DataType.Command, PING.length, new Data(PING));
                StarShip ship = new StarShip(StarShip.SLOWER, ping, null);
                addTask(ship);
            }
            // try heartbeat next 2 seconds
            expired = now + 2000;
        }
        // idling
        assert IDLE_INTERVAL > 0 : "IDLE_INTERVAL error: " + IDLE_INTERVAL;
        _sleep(IDLE_INTERVAL);
    }
    private long expired = (new Date()).getTime();

    private static void _sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static final byte[] PING = {'P', 'I', 'N', 'G'};
    private static final byte[] PONG = {'P', 'O', 'N', 'G'};
    private static final byte[] AGAIN = {'A', 'G', 'A', 'I', 'N'};
    private static final byte[] OK = {'O', 'K'};
}
