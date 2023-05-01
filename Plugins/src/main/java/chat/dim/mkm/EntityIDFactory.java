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

import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
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
        assert address != null : "ID.address should not be empty: " + this;
        // compatible with MKM 0.9.*
        return NetworkID.getType(address.getType());
    }

    @Override
    public boolean isBroadcast() {
        assert address != null : "ID.address should not be empty: " + this;
        return address.isBroadcast();
    }

    @Override
    public boolean isUser() {
        assert address != null : "ID.address should not be empty: " + this;
        return address.isUser();
    }

    @Override
    public boolean isGroup() {
        assert address != null : "ID.address should not be empty: " + this;
        return address.isGroup();
    }
}

public final class EntityIDFactory extends IDFactory {

    @Override
    protected ID newID(String identifier, String name, Address address, String terminal) {
        return new EntityID(identifier, name, address, terminal);
    }

    @Override
    protected ID parse(String identifier) {
        if (identifier == null || identifier.length() == 0) {
            throw new NullPointerException("ID empty");
        } else if (ID.ANYONE.equalsIgnoreCase(identifier)) {
            return ID.ANYONE;
        } else if (ID.EVERYONE.equalsIgnoreCase(identifier)) {
            return ID.EVERYONE;
        } else if (ID.FOUNDER.equalsIgnoreCase(identifier)) {
            return ID.FOUNDER;
        }
        return super.parse(identifier);
    }
}
