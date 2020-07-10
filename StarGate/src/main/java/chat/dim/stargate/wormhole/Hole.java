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
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Header;
import chat.dim.mtp.protocol.Package;
import chat.dim.stargate.StarDelegate;
import chat.dim.tcp.*;
import chat.dim.tlv.Data;

public class Hole extends Thread implements ConnectionHandler {

    private boolean running = false;

    // connections
    private final Set<Connection> connections = new LinkedHashSet<>();
    private final ReadWriteLock connectionLock = new ReentrantReadWriteLock();
    private final Map<SocketAddress, WeakReference<Connection>> connectionMap = new HashMap<>();

    // declaration forms
    private final List<SocketAddress> declarations = new ArrayList<>();
    private final ReadWriteLock declarationLock = new ReentrantReadWriteLock();

    private WeakReference<StarDelegate> delegateRef = null;

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
    //  Connections
    //

    public Connection getConnection(SocketAddress address) {
        WeakReference<Connection> ref = connectionMap.get(address);
        if (ref == null) {
            return null;
        }
        return ref.get();
    }

    protected Connection createConnection(Socket socket, String host, int port) {
        Connection connection;
        if (socket == null) {
            connection = new ClientConnection(host, port);
        } else {
            connection = new ServerConnection(socket);
        }
        connection.start();
        return connection;
    }

    private Connection connect(Socket socket, String host, int port) {
        Connection connection = null;
        Lock writeLock = connectionLock.writeLock();
        writeLock.lock();
        try {
            for (Connection item : connections) {
                if (item.port == port && item.host.equals(host)) {
                    connection = item;
                    break;
                }
            }
            if (connection == null) {
                connection = createConnection(socket, host, port);
                connections.add(connection);
                // mapping for "address -> connection"
                connectionMap.put(connection.address, new WeakReference<>(connection));
            }
        } finally {
            writeLock.unlock();
        }
        return connection;
    }

    /**
     *  Create a connection for server
     *
     * @param socket - socket accepted for client
     * @return connection
     */
    public Connection connect(Socket socket) {
        String host = socket.getInetAddress().getHostAddress();
        int port = socket.getPort();
        return connect(socket, host, port);
    }

    /**
     *  Create a connection for client
     *
     * @param host - server host
     * @param port - server port
     * @return connection
     */
    public Connection connect(String host, int port) {
        return connect(null, host, port);
    }

    //
    //  Declaration forms
    //

    private void addDeclaration(SocketAddress address) {
        Lock writeLock = declarationLock.writeLock();
        writeLock.lock();
        try {
            declarations.add(address);
        } finally {
            writeLock.unlock();
        }
    }

    private SocketAddress getDeclaration() {
        SocketAddress address = null;
        Lock writeLock = declarationLock.writeLock();
        writeLock.lock();
        try {
            if (declarations.size() > 0) {
                address = declarations.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return address;
    }

    /**
     *  Send data to remote address
     *
     * @param payload - data to be sent
     * @param remoteAddress - remote IP and port
     * @return -1 on error
     */
    public int send(byte[] payload, SocketAddress remoteAddress) {
        Connection connection = getConnection(remoteAddress);
        if (connection == null) {
            return -1;
        }
        // packing for D-MTP
        Data body = new Data(payload);
        Package pack = Package.create(DataType.Message, body.getLength(), body);
        return connection.send(pack.getBytes());
    }

    private Cargo receive(Connection connection) {
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
        return new Cargo(data.getBytes(headLen), connection.address, null);
    }

    private Cargo receive() {
        Cargo cargo = null;
        Connection conn;
        SocketAddress address;
        // 1. try by declaration forms
        while (true) {
            address = getDeclaration();
            if (address == null) {
                // no more declaration form
                break;
            }
            conn = getConnection(address);
            if (conn == null) {
                // connection lost?
                continue;
            }
            cargo = receive(conn);
            if (cargo != null) {
                // got one
                return cargo;
            }
        }
        // 2. try every connection
        Lock readLock = connectionLock.readLock();
        readLock.lock();
        try {
            for (Connection item : connections) {
                cargo = receive(item);
                if (cargo != null) {
                    // got one
                    break;
                }
            }
        } finally {
            readLock.unlock();
        }
        return cargo;
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
        Cargo cargo;
        List<byte[]> responses;
        while (running) {
            cargo = receive();
            if (cargo == null) {
                // received nothing, have a rest ^_^
                _sleep(200);
            } else {
                // dispatch
                responses = dispatch(cargo.payload, cargo.source);
                for (byte[] res : responses) {
                    send(res, cargo.source);
                }
            }

        }
    }

    private List<byte[]> dispatch(byte[] payload, SocketAddress source) {
        List<byte[]> responses = new ArrayList<>();
        // TODO: call the listeners
        return responses;
    }

    private void ping() {
        long now = (new Date()).getTime();
        Lock readLock = connectionLock.readLock();
        readLock.lock();
        try {
            for (Connection item : connections) {
                if (item.isExpired(now)) {
                    item.send(HEARTBEAT);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    private static final byte[] PING = {'P', 'I', 'N', 'G'};
    private static final byte[] HEARTBEAT = Package.create(DataType.Message, 4, new Data(PING)).getBytes();

    //
    //  ConnectionHandler
    //

    @Override
    public void onConnectionStatusChanged(Connection connection, ConnectionStatus oldStatus, ConnectionStatus newStatus) {
    }

    @Override
    public void onConnectionReceivedData(Connection connection) {
        addDeclaration(connection.address);
    }

    @Override
    public void onConnectionOverflowed(Connection connection, byte[] ejected) {
    }
}
