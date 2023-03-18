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

import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.Visa;

/**
 *  DIM Server
 */
public class Station implements User {

    public static ID ANY = ID.create("station", Address.ANYWHERE, null);
    public static ID EVERY = ID.create("stations", Address.EVERYWHERE, null);

    // inner user
    private User user;

    private String host;
    private int port;

    public Station(ID identifier, String host, int port) {
        super();
        assert EntityType.STATION.equals(identifier.getType()) : "station ID error: " + identifier;
        this.user = new BaseUser(identifier);
        this.host = host;
        this.port = port;
    }

    public Station(ID identifier) {
        this(identifier, null, 0);
    }

    public Station(String host, int port) {
        super();
        this.user = new BaseUser(ANY);
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) {
            // same object
            return true;
        } else {
            // check with inner user
            return user.equals(other);
        }
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        ID identifier = getIdentifier();
        int network = identifier.getAddress().getType();
        return "<" + className + " id=\"" + identifier + "\" network=" + network +
                " host=\"" + getHost() + "\" port=" + getPort() + " />";
    }

    public void setIdentifier(ID identifier) {
        DataSource delegate = getDataSource();
        User inner = new BaseUser(identifier);
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
        return (DataSource) user.getDataSource();
    }

    @Override
    public Meta getMeta() {
        return user.getMeta();
    }

    @Override
    public Document getDocument(String type) {
        return user.getDocument(type);
    }

    //-------- User

    @Override
    public Visa getVisa() {
        return user.getVisa();
    }

    @Override
    public List<ID> getContacts() {
        return user.getContacts();
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        return user.verify(data, signature);
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
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

    //-------- Server

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
        }
        return port;
    }

    /**
     *  Get provider ID
     *
     * @return ISP ID, station group
     */
    public ID getProvider() {
        Document doc = getDocument("*");
        if (doc == null) {
            return null;
        }
        return ID.parse(doc.getProperty("ISP"));
    }}
