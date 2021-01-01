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

import java.lang.ref.WeakReference;

import chat.dim.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.NetworkType;

public class Station extends User {

    private WeakReference<Delegate> delegateRef = null;

    private String host;
    private int port;

    public Station(ID identifier) {
        this(identifier, null, 0);
    }

    public Station(ID identifier, String host, int port) {
        super(identifier);
        assert NetworkType.STATION.equals(identifier.getType()) : "station ID error: " + identifier;
        this.host = host;
        this.port = port;
    }

    public Delegate getDelegate() {
        if (delegateRef == null) {
            return null;
        }
        return delegateRef.get();
    }

    public void setDelegate(Delegate delegate) {
        delegateRef = new WeakReference<>(delegate);
    }

    /**
     *  Station IP
     *
     * @return IP address
     */
    public String getHost() {
        if (host == null) {
            Document doc = getDocument("*");
            if (doc != null) {
                Object value = doc.getProperty("host");
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
            Document doc = getDocument("*");
            if (doc != null) {
                Object value = doc.getProperty("port");
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

    /**
     *  Station Delegate
     *  ~~~~~~~~~~~~~~~~
     */
    public interface Delegate {

        /**
         *  Received a new data package from the station
         *
         * @param data - data package received
         * @param server - current station
         */
        void onReceivePackage(byte[] data, Station server);

        /**
         *  Send data package to station success
         *
         * @param data - data package sent
         * @param server - current station
         */
        void didSendPackage(byte[] data, Station server);

        /**
         *  Failed to send data package to station
         *
         * @param error - error information
         * @param data - data package to send
         * @param server - current station
         */
        void didFailToSendPackage(Error error, byte[] data, Station server);

        /**
         *  Callback for handshake accepted
         *
         * @param server - current station
         */
        void onHandshakeAccepted(Station server);
    }
}
