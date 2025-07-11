/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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

/**
 *  General ID Factory
 */
public class IdentifierFactory implements ID.Factory {

    protected final Map<String, ID> identifiers = new HashMap<>();

    @Override
    public ID generateIdentifier(Meta meta, int network, String terminal) {
        Address address = Address.generate(meta, network);
        assert address != null : "failed to generate ID with meta: " + meta.toMap();
        return ID.create(meta.getSeed(), address, terminal);
    }

    @Override
    public ID createIdentifier(String name, Address address, String terminal) {
        String identifier = Identifier.concat(name, address, terminal);
        ID did = identifiers.get(identifier);
        if (did == null) {
            did = newID(identifier, name, address, terminal);
            identifiers.put(identifier, did);
        }
        return did;
    }

    @Override
    public ID parseIdentifier(String identifier) {
        ID did = identifiers.get(identifier);
        if (did == null) {
            did = parse(identifier);
            if (did != null) {
                identifiers.put(identifier, did);
            }
        }
        return did;
    }

    // override for customized ID
    protected ID newID(String identifier, String name, Address address, String terminal) {
        return new Identifier(identifier, name, address, terminal);
    }

    protected ID parse(final String identifier) {
        String name;
        Address address;
        String terminal;
        // split ID string
        String[] pair = identifier.split("/");
        assert pair[0].length() > 0 : "ID error: " + identifier;
        // terminal
        if (pair.length == 1) {
            // no terminal
            terminal = null;
        } else {
            // got terminal
            //assert pair.length == 2 : "ID error: " + identifier;
            terminal = pair[1];
            assert terminal.length() > 0 : "ID.terminal error: " + identifier;
        }
        // name @ address
        pair = pair[0].split("@");
        assert pair[0].length() > 0 : "ID error: " + identifier;
        if (pair.length == 1) {
            // got address without name
            name = null;
            address = Address.parse(pair[0]);
        } else if (pair.length == 2) {
            // got name & address
            name = pair[0];
            address = Address.parse(pair[1]);
        } else {
            assert false : "ID error: " + identifier;
            return null;
        }
        if (address == null) {
            assert false : "cannot get address from id: " + identifier;
            return null;
        }
        return newID(identifier, name, address, terminal);
    }

}
