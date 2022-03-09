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
import chat.dim.net.Connection;
import chat.dim.net.Hub;
import chat.dim.port.Departure;
import chat.dim.port.Docker;
import chat.dim.port.Ship;
import chat.dim.skywalker.Runner;
import chat.dim.startrek.StarGate;
import chat.dim.threading.Daemon;

public abstract class CommonGate<H extends Hub> extends StarGate implements Runnable {

    private H hub = null;

    private final Daemon daemon;
    private boolean running;

    public CommonGate(Docker.Delegate delegate, boolean isDaemon) {
        super(delegate);
        daemon = new Daemon(this, isDaemon);
        running = false;
    }
    protected CommonGate(Docker.Delegate delegate) {
        this(delegate, true);
    }

    public H getHub() {
        return hub;
    }
    public void setHub(H h) {
        hub = h;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        stop();
        running = true;
        daemon.start();
    }

    public void stop() {
        running = false;
        daemon.stop();
    }

    @Override
    public void run() {
        running = true;
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
        try {
            boolean incoming = getHub().process();
            boolean outgoing = super.process();
            return incoming || outgoing;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    //
    //  Docker
    //

    @Override
    public Docker getDocker(SocketAddress remote, SocketAddress local) {
        return super.getDocker(remote, null);
    }

    @Override
    protected void setDocker(SocketAddress remote, SocketAddress local, Docker docker) {
        super.setDocker(remote, null, docker);
    }

    @Override
    protected void removeDocker(SocketAddress remote, SocketAddress local, Docker docker) {
        super.removeDocker(remote, null, docker);
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
/*/
    @Override
    protected void heartbeat(Connection connection) {
        // let the client to do the job
        if (connection instanceof ActiveConnection) {
            super.heartbeat(connection);
        }
    }
/*/
    @Override
    public void onError(Throwable error, byte[] data, SocketAddress source, SocketAddress destination, Connection connection) {
        // ignore
    }

    protected Docker getDocker(SocketAddress remote, SocketAddress local, List<byte[]> data) {
        Docker docker = getDocker(remote, local);
        if (docker == null) {
            Connection conn = getHub().connect(remote, local);
            if (conn != null) {
                docker = createDocker(data, remote, local, conn);
                assert docker != null : "failed to create docker: " + remote + ", " + local;
                setDocker(docker.getRemoteAddress(), docker.getLocalAddress(), docker);
            }
        }
        return docker;
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
