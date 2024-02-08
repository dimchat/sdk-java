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
package chat.dim.network;

import java.net.SocketAddress;
import java.util.List;

import chat.dim.mtp.MTPHelper;
import chat.dim.mtp.Package;
import chat.dim.mtp.StreamArrival;
import chat.dim.mtp.StreamDocker;
import chat.dim.mtp.TransactionID;
import chat.dim.net.Channel;
import chat.dim.net.Connection;
import chat.dim.net.Hub;
import chat.dim.port.Arrival;
import chat.dim.port.Docker;
import chat.dim.tcp.StreamHub;
import chat.dim.type.Data;

/**
 *  Gate with hub for connection
 */
public abstract class CommonGate extends BaseGate<StreamHub> /*implements Runnable */{

    private boolean running;

    public CommonGate(Docker.Delegate delegate) {
        super(delegate);
        running = false;
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
    /*/
    @Override
    public void run() {
        //running = true;
        while (isRunning()) {
            if (!process()) {
                idle();
            }
        }
        // gate closing
    }

    protected void idle() {
        idle(256);
    }

    public static void idle(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /*/

    public Channel getChannel(SocketAddress remote, SocketAddress local) {
        Hub hub = getHub();
        assert hub != null : "no hub for channel: " + remote + ", " + local;
        return hub.open(remote, local);
    }

    public boolean sendResponse(byte[] payload, Arrival ship, SocketAddress remote, SocketAddress local) {
        assert ship instanceof StreamArrival : "arrival ship error: " + ship;
        //MTPStreamArrival arrival = (MTPStreamArrival) ship;
        Docker docker = getDocker(remote, local);
        assert docker instanceof StreamDocker : "docker error: " + docker;
        StreamDocker worker = (StreamDocker) docker;
        //TransactionID sn = TransactionID.from(new Data(arrival.getSN()));
        TransactionID sn = TransactionID.generate();
        Package pack = MTPHelper.createMessage(sn, new Data(payload));
        return worker.sendPackage(pack);
    }

    public Docker fetchDocker(SocketAddress remote, SocketAddress local, List<byte[]> advanceParty) {
        Docker docker = getDocker(remote, local);
        if (docker == null && advanceParty != null) {
            Connection conn = getHub().connect(remote, local);
            if (conn != null) {
                docker = createDocker(conn, advanceParty);
                if (docker == null) {
                    assert false : "failed to create docker: " + remote + ", " + local;
                } else {
                    setDocker(remote, local, docker);
                }
            }
        }
        return docker;
    }

}
