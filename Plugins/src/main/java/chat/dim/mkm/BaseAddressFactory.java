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
import chat.dim.protocol.Meta;

/**
 *  Base Address Factory
 */
public class BaseAddressFactory implements Address.Factory {

    protected final Map<String, Address> addresses = new HashMap<>();

    @Override
    public Address generateAddress(Meta meta, int network) {
        Address address = meta.generateAddress(network);
        if (address != null) {
            addresses.put(address.toString(), address);
        }
        return address;
    }

    @Override
    public Address parseAddress(String address) {
        Address add = addresses.get(address);
        if (add == null) {
            add = parse(address);
            if (add != null) {
                addresses.put(address, add);
            }
        }
        return add;
    }

    protected Address parse(String address) {
        if (address == null) {
            //throw new NullPointerException("address empty");
            assert false : "address empty";
            return null;
        }
        int len = address.length();
        if (len == 0) {
            assert false : "address empty";
            return null;
        } else if (len == 8) {
            // "anywhere"
            if (Address.ANYWHERE.equalsIgnoreCase(address)) {
                return Address.ANYWHERE;
            }
        } else if (len == 10) {
            // "everywhere"
            if (Address.EVERYWHERE.equalsIgnoreCase(address)) {
                return Address.EVERYWHERE;
            }
        }
        Address res;
        if (26 <= len && len <= 35) {
            // BTC
            res = BTCAddress.parse(address);
        } else if (len == 42) {
            // ETH
            res = ETHAddress.parse(address);
        } else {
            //throw new AssertionError("invalid address: " + address);
            assert false : "invalid address: " + address;
            res = null;
        }
        // TODO: other types of address
        assert res != null : "invalid address: " + address;
        return res;
    }

}
