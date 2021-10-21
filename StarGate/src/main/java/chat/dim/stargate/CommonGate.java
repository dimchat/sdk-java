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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import chat.dim.mtp.MTPHelper;
import chat.dim.mtp.Package;
import chat.dim.mtp.PackageDeparture;
import chat.dim.net.BaseConnection;
import chat.dim.net.Connection;
import chat.dim.net.ConnectionState;
import chat.dim.net.Hub;
import chat.dim.port.Departure;
import chat.dim.port.Docker;
import chat.dim.port.Ship;
import chat.dim.skywalker.Runner;
import chat.dim.startrek.StarGate;

public abstract class CommonGate<H extends Hub> extends StarGate implements Runnable {

    private boolean running = false;
    private H hub = null;

    protected CommonGate(Delegate delegate) {
        super(delegate);
    }

    public H getHub() {
        return hub;
    }
    public void setHub(H h) {
        hub = h;
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        while (isRunning()) {
            if (!process()) {
                idle();
            }
        }
    }

    protected void idle() {
        Runner.idle(128);
    }

    @Override
    public boolean process() {
        boolean incoming = getHub().process();
        boolean outgoing = super.process();
        return incoming || outgoing;
    }

    @Override
    public Connection getConnection(SocketAddress remote, SocketAddress local) {
        return getHub().connect(remote, local);
    }

    @Override
    protected List<byte[]> cacheAdvanceParty(byte[] data, SocketAddress source, SocketAddress destination, Connection connection) {
        // TODO: cache the advance party before decide which docker to use
        List<byte[]> array = new ArrayList<>();
        if (data != null) {
            array.add(data);
        }
        return array;
    }

    @Override
    protected void clearAdvanceParty(SocketAddress source, SocketAddress destination, Connection connection) {
        // TODO: remove advance party for this connection
    }

    @Override
    protected void heartbeat(Connection connection) {
        // let the client to do the job
        if (connection instanceof BaseConnection) {
            if (((BaseConnection) connection).isActivated) {
                super.heartbeat(connection);
            }
        }
    }

    private void kill(SocketAddress remote, SocketAddress local, Connection connection) {
        // if conn is null, disconnect with (remote, local);
        // else, disconnect with connection when local address matched.
        connection = getHub().disconnect(remote, local, connection);
        // if connection is not activated, means it's a server connection,
        // remove the docker too.
        if (connection instanceof BaseConnection) {
            if (!((BaseConnection) connection).isActivated) {
                // remove docker for server connection
                remote = connection.getRemoteAddress();
                local = connection.getLocalAddress();
                removeDocker(remote, local, null);
            }
        }
    }

    @Override
    public void onStateChanged(ConnectionState previous, ConnectionState current, Connection connection) {
        super.onStateChanged(previous, current, connection);
        if (current != null && current.equals(ConnectionState.ERROR)) {
            kill(null, null, connection);
        }
    }

    @Override
    public void onError(Throwable error, byte[] data, SocketAddress source, SocketAddress destination, Connection connection) {
        if (connection == null) {
            // failed to receive data
            kill(source, destination, null);
        } else {
            // failed to send data
            kill(destination, source, connection);
        }
    }

    protected Docker getDocker(SocketAddress remote, SocketAddress local, List<byte[]> data) {
        Docker worker = getDocker(remote, local);
        if (worker == null) {
            worker = createDocker(remote, local, data);
            if (worker != null) {
                putDocker(worker);
            }
        }
        return worker;
    }

    public boolean send(SocketAddress source, SocketAddress destination,
                        Departure ship) {
        Docker worker = getDocker(destination, source, null);
        return worker != null && worker.appendDeparture(ship);
    }

    public boolean send(SocketAddress source, SocketAddress destination,
                        Package pack, int priority, Ship.Delegate delegate) {
        Departure ship = new PackageDeparture(pack, priority, delegate);
        return send(source, destination, ship);
    }

    public boolean send(SocketAddress source, SocketAddress destination,
                        byte[] payload, int priority, Ship.Delegate delegate) {
        Package pack = MTPHelper.createMessage(payload);
        return send(source, destination, pack, priority, delegate);
    }

    public boolean send(SocketAddress source, SocketAddress destination,
                        byte[] payload, Ship.Delegate delegate) {
        return send(source, destination, payload, NORMAL, delegate);
    }
    public boolean send(SocketAddress source, SocketAddress destination,
                        Package pack) {
        return send(source, destination, pack, NORMAL, getDelegate());
    }
    static final int NORMAL = Departure.Priority.NORMAL.value;
}
