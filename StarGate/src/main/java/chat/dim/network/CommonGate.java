/* license: https://mit-license.org
 *
 *  Star Gate: Network Connection Module
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim.network;

import java.net.SocketAddress;

import chat.dim.net.Connection;
import chat.dim.net.Hub;
import chat.dim.port.Arrival;
import chat.dim.port.Porter;
import chat.dim.socket.ActiveConnection;
import chat.dim.startrek.StarGate;

/**
 *  Gate with hub for connection
 */
public abstract class CommonGate<H extends Hub>
        extends StarGate {

    private H hub;

    public CommonGate(Porter.Delegate keeper) {
        super(keeper);
    }

    public H getHub() {
        return hub;
    }
    public void setHub(H h) {
        hub = h;
    }

    //
    //  Docker
    //

    @Override
    protected Porter getPorter(SocketAddress remote, SocketAddress local) {
        return super.getPorter(remote, null);
    }

    @Override
    protected Porter setPorter(SocketAddress remote, SocketAddress local, Porter porter) {
        return super.setPorter(remote, null, porter);
    }

    @Override
    protected Porter removePorter(SocketAddress remote, SocketAddress local, Porter porter) {
        return super.removePorter(remote, null, porter);
    }

    public Porter fetchPorter(SocketAddress remote, SocketAddress local) {
        // get connection from hub
        Connection conn = getHub().connect(remote, local);
        if (conn == null) {
            assert false : "failed to get connection: " + local + " -> " + remote;
            return null;
        }
        // connected, get docker with this connection
        return dock(conn, true);
    }

    public boolean sendResponse(byte[] payload, Arrival ship, SocketAddress remote, SocketAddress local) {
        Porter docker = getPorter(remote, local);
        if (docker == null) {
            return false;
        } else if (!docker.isAlive()) {
            return false;
        }
        return docker.sendData(payload);
    }

    //
    //  Keep Active
    //

    @Override
    protected void heartbeat(Connection connection) {
        // let the client to do the job
        if (connection instanceof ActiveConnection) {
            super.heartbeat(connection);
        }
    }

}
