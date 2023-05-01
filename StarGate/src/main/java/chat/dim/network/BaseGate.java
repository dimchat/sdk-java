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
import java.util.ArrayList;
import java.util.List;

import chat.dim.net.Connection;
import chat.dim.net.Hub;
import chat.dim.port.Docker;
import chat.dim.startrek.StarGate;

public abstract class BaseGate<H extends Hub>
        extends StarGate {

    private H hub;

    public BaseGate(Docker.Delegate delegate) {
        super(delegate);
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

    public Docker getDocker(SocketAddress remote, SocketAddress local, List<byte[]> advanceParty) {
        Docker docker = getDocker(remote, local);
        if (docker == null && advanceParty != null) {
            Connection conn = getHub().connect(remote, local);
            if (conn != null) {
                docker = createDocker(conn, advanceParty);
                assert docker != null : "failed to create docker: " + remote + ", " + local;
                setDocker(remote, local, docker);
            }
        }
        return docker;
    }

    @Override
    protected Docker getDocker(SocketAddress remote, SocketAddress local) {
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
    protected List<byte[]> cacheAdvanceParty(byte[] data, Connection connection) {
        // TODO: cache the advance party before decide which docker to use
        List<byte[]> array = new ArrayList<>();
        if (data != null && data.length > 0) {
            array.add(data);
        }
        return array;
    }

    @Override
    protected void clearAdvanceParty(Connection connection) {
        // TODO: remove advance party for this connection
    }
}
