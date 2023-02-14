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

import java.util.List;

import chat.dim.mtp.StreamDocker;
import chat.dim.net.Connection;
import chat.dim.port.Docker;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

public final class TCPServerGate extends CommonGate {

    public TCPServerGate(Docker.Delegate delegate) {
        super(delegate);
    }

    @Override
    protected Docker createDocker(Connection conn, List<byte[]> advanceParty) {
        int count = advanceParty == null ? 0 : advanceParty.size();
        if (count == 0) {
            return null;
        }
        ByteArray data = new Data(advanceParty.get(0));
        for (int i = 1; i < count; ++i) {
            data = data.concat(advanceParty.get(i));
        }
        if (data.getSize() == 0) {
            return null;
        }
        // check data format before creating docker
        if (StreamDocker.check(data)) {
            StreamDocker docker = new StreamDocker(conn);
            docker.setDelegate(getDelegate());
            return docker;
        }
        throw new AssertionError("failed to create docker: " + data);
    }
}
