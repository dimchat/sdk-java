/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
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
package chat.dim.mkm;

import java.util.HashMap;
import java.util.Map;

import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkID;
import chat.dim.type.ConstantString;

/**
 *  ID for entity (User/Group)
 *
 *      data format: "name@address[/terminal]"
 *
 *      fields:
 *          name     - entity name, the seed of fingerprint to build address
 *          address  - a string to identify an entity
 *          terminal - entity login resource(device), OPTIONAL
 */
final class EntityID extends ConstantString implements ID {

    private final String name;
    private final Address address;
    private final String terminal;

    public EntityID(String identifier, String name, Address address, String terminal) {
        super(identifier);
        this.name = name;
        this.address = address;
        this.terminal = terminal;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public String getTerminal() {
        return terminal;
    }

    /**
     *  Get Network ID
     *
     * @return address type as network ID
     */
    @Override
    public int getType() {
        assert address != null : "ID.address should not be empty: " + toString();
        byte network = (byte) address.getType();
        // compatible with MKM 0.9.*
        return NetworkID.getType(network);
    }

    @Override
    public boolean isBroadcast() {
        assert address != null : "ID.address should not be empty: " + toString();
        return address.isBroadcast();
    }

    @Override
    public boolean isUser() {
        assert address != null : "ID.address should not be empty: " + toString();
        return address.isUser();
    }

    @Override
    public boolean isGroup() {
        assert address != null : "ID.address should not be empty: " + toString();
        return address.isGroup();
    }
}

final class EntityIDFactory implements ID.Factory {

    private final Map<String, ID> identifiers = new HashMap<>();

    @Override
    public ID generateID(Meta meta, int network, String terminal) {
        Address address = Address.generate(meta, network);
        assert address != null : "failed to generate ID with meta: " + meta.toMap();
        return ID.create(meta.getSeed(), address, terminal);
    }

    @Override
    public ID createID(String name, Address address, String terminal) {
        String identifier = concat(name, address, terminal);
        ID id = identifiers.get(identifier);
        if (id == null) {
            id = new EntityID(identifier, name, address, terminal);
            identifiers.put(identifier, id);
        }
        return id;
    }

    @Override
    public ID parseID(String identifier) {
        ID id = identifiers.get(identifier);
        if (id == null) {
            id = parse(identifier);
            if (id != null) {
                identifiers.put(identifier, id);
            }
        }
        return id;
    }

    private static String concat(String name, Address address, String terminal) {
        String string = address.toString();
        if (name != null && name.length() > 0) {
            string = name + "@" + string;
        }
        if (terminal != null && terminal.length() > 0) {
            string = string + "/" + terminal;
        }
        return string;
    }

    private static ID parse(final String string) {
        String name;
        Address address;
        String terminal;
        // split ID string
        String[] pair = string.split("/");
        // terminal
        if (pair.length == 1) {
            // no terminal
            terminal = null;
        } else {
            // got terminal
            assert pair.length == 2 : "ID error: " + string;
            assert pair[1].length() > 0 : "ID.terminal error: " + string;
            terminal = pair[1];
        }
        // name @ address
        assert pair[0].length() > 0 : "ID error: " + string;
        pair = pair[0].split("@");
        assert pair[0].length() > 0 : "ID error: " + string;
        if (pair.length == 1) {
            // got address without name
            name = null;
            address = Address.parse(pair[0]);
        } else {
            // got name & address
            assert pair.length == 2 : "ID error: " + string;
            assert pair[1].length() > 0 : "ID.address error: " + string;
            name = pair[0];
            address = Address.parse(pair[1]);
        }
        if (address == null) {
            return null;
        }
        return new EntityID(string, name, address, terminal);
    }
}
