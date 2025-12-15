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
package chat.dim.mkm;

import java.util.List;
import java.util.Map;

import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.Visa;
import chat.dim.type.Converter;

/**
 *  DIM Server
 */
public class Station implements User {

    /**
     *  Broadcast
     */
    public static ID ANY = Identifier.create("station", Address.ANYWHERE, null);
    public static ID EVERY = Identifier.create("stations", Address.EVERYWHERE, null);

    // inner user
    private User user;

    private String host;
    private int port;

    protected List<Document> documents;

    public Station(ID sid, String host, int port) {
        super();
        assert EntityType.STATION.equals(sid.getType()) || EntityType.ANY.equals(sid.getType())
                : "station ID error: " + sid;
        this.user = new BaseUser(sid);
        this.host = host;
        this.port = port;
        this.documents = null;
    }

    public Station(ID sid) {
        this(sid, null, 0);
    }

    public Station(String host, int port) {
        this(ANY, host, port);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Station) {
            return sameStation((Station) other, this);
        }
        // others?
        return user.equals(other);
    }

    @Override
    public int hashCode() {
        return user.hashCode();
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        ID sid = getIdentifier();
        int network = sid.getAddress().getNetwork();
        return "<" + className + " id=\"" + sid + "\" network=" + network +
                " host=\"" + getHost() + "\" port=" + getPort() + " />";
    }

    /**
     *  Reload station info: host &amp; port, SP ID
     */
    public void reload() {
        documents = getDocuments();
        // update station host
        if (host == null) {
            String docHost = Converter.getString(getProfile("host"));
            if (docHost != null) {
                host = docHost;
            }
        }
        // update station port
        if (port == 0) {
            Integer docPort = Converter.getInteger(getProfile("port"));
            if (docPort != null && docPort > 0) {
                assert 16 < docPort && docPort < 65536 : "station port error: " + docPort;
                port = docPort;
            }
        }
    }

    /**
     *  Get last property
     */
    public Object getProfile(String key) {
        List<Document> docs = documents;
        if (docs == null) {
            return null;
        }
        // TODO: sort by doc.time DESC
        Object value;
        for (Document doc : docs) {
            value = doc.getProperty(key);
            if (value != null) {
                return value;
            }
        }
        // property not found
        return null;
    }

    /**
     *  Get provider ID
     *
     * @return ISP ID, station group
     */
    public ID getProvider() {
        return ID.parse(getProfile("provider"));
    }

    /**
     *  Station IP
     */
    public String getHost() {
        return host;
    }

    /**
     *  Station Port
     */
    public int getPort() {
        return port;
    }

    public void setIdentifier(ID sid) {
        DataSource delegate = getDataSource();
        User inner = new BaseUser(sid);
        inner.setDataSource(delegate);
        user = inner;
    }

    //-------- Entity

    @Override
    public ID getIdentifier() {
        return user.getIdentifier();
    }

    @Override
    public int getType() {
        return user.getType();
    }

    @Override
    public void setDataSource(Entity.DataSource dataSource) {
        user.setDataSource(dataSource);
    }

    @Override
    public DataSource getDataSource() {
        Entity.DataSource facebook = user.getDataSource();
        if (facebook instanceof DataSource) {
            return (DataSource) facebook;
        }
        assert facebook == null : "user data source error: " + facebook;
        return null;
    }

    @Override
    public Meta getMeta() {
        return user.getMeta();
    }

    @Override
    public List<Document> getDocuments() {
        return user.getDocuments();
    }

    //-------- User

    @Override
    public List<ID> getContacts() {
        return user.getContacts();
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        return user.verify(data, signature);
    }

    @Override
    public Map<String, byte[]> encrypt(byte[] plaintext) {
        return user.encrypt(plaintext);
    }

    @Override
    public byte[] sign(byte[] data) {
        return user.sign(data);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return user.decrypt(ciphertext);
    }

    @Override
    public Visa sign(Visa doc) {
        return user.sign(doc);
    }

    @Override
    public boolean verify(Visa doc) {
        return user.verify(doc);
    }

    //
    //  Comparison
    //

    public static boolean sameStation(Station a, Station b) {
        if (a == b) {
            // same object
            return true;
        }
        return checkIdentifiers(a.getIdentifier(), b.getIdentifier()) &&
                checkHosts(a.getHost(), b.getHost()) &&
                checkPorts(a.getPort(), b.getPort());
    }

    private static boolean checkIdentifiers(ID a, ID b) {
        if (a == b) {
            // same object
            return true;
        } else if (a.isBroadcast() || b.isBroadcast()) {
            return true;
        }
        return a.equals(b);
    }
    private static boolean checkHosts(String a, String b) {
        if (a == null || b == null) {
            return true;
        } else if (a.isEmpty() || b.isEmpty()) {
            return true;
        }
        return a.equals(b);
    }
    private static boolean checkPorts(int a, int b) {
        if (a == 0 || b == 0) {
            return true;
        }
        return a == b;
    }

}
