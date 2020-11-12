/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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

import chat.dim.User;
import chat.dim.protocol.ID;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.Profile;

public class Station extends User {

    private String host;
    private int port;

    public Station(ID identifier) {
        this(identifier, null, 0);
    }

    public Station(ID identifier, String host, int port) {
        super(identifier);
        assert NetworkType.Station.equals(identifier.getType()) : "station ID error: " + identifier;
        this.host = host;
        this.port = port;
    }

    /**
     *  Station IP
     *
     * @return IP address
     */
    public String getHost() {
        if (host == null) {
            Profile profile = getProfile();
            if (profile != null) {
                Object value = profile.getProperty("host");
                if (value != null) {
                    host = (String) value;
                }
            }
            if (host == null) {
                host = "0.0.0.0";
            }
        }
        return host;
    }

    /**
     *  Station Port
     *
     * @return port number
     */
    public int getPort() {
        if (port == 0) {
            Profile profile = getProfile();
            if (profile != null) {
                Object value = profile.getProperty("port");
                if (value != null) {
                    port = (int) value;
                }
            }
            if (port == 0) {
                port = 9394;
            }
        }
        return port;
    }
}
