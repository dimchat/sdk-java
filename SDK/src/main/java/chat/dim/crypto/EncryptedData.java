/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2025 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Albert Moky
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
package chat.dim.crypto;

import java.util.Map;
import java.util.Set;

import chat.dim.mkm.Identifier;
import chat.dim.protocol.ID;
import chat.dim.protocol.TransportableData;

/**
 *  User Encrypted Key Data with Terminals
 */
public interface EncryptedData {

    Map<String, byte[]> toMap();

    boolean isEmpty();

    /**
     *  Put encrypted key data for terminal
     *
     * @param terminal - ID terminal
     * @param data     - encrypted key data
     */
    byte[] put(String terminal, byte[] data);

    /**
     *  Remove encrypted key data for terminal
     *
     * @param terminal - ID terminal
     * @return removed data
     */
    byte[] remove(String terminal);

    /**
     *  Get encrypted key data for terminal
     *
     * @param terminal - ID terminal
     * @return encrypted key data
     */
    byte[] get(String terminal);

    /**
     *  Get all data values
     *
     * @return data list
     */
    Set<byte[]> values();

    /**
     *  Encode key data
     *
     * @param did - user ID
     * @return encoded key data with target (ID + terminal)
     */
    Map<String, Object> encode(ID did);

    /**
     *  Decode key data from 'message.keys'
     *
     * @param keys      - encoded key data with target (ID + terminal)
     * @param did       - receiver ID
     * @param terminals - visa terminals
     * @return decrypted key data with terminals
     */
    static EncryptedData decode(Map<String, Object> keys, ID did, Iterable<String> terminals) {
        EncryptedData result = new UserEncryptedData();
        //
        //  0. ID string without terminal
        //
        String identifier = Identifier.concat(did.getName(), did.getAddress(), null);
        String target;
        Object base64;
        TransportableData ted;
        byte[] data;
        for (String item : terminals) {
            target = item == null || item.isEmpty() ? "*" : item;
            //
            //  1. get encoded data with target (ID + terminal)
            //
            if (target.equals("*")) {
                base64 = keys.get(identifier);
            } else {
                base64 = keys.get(identifier + "/" + target);
            }
            if (base64 == null) {
                // key data not found
                continue;
            }
            //
            //  2. decode data
            //
            ted = TransportableData.parse(base64);
            data = ted == null ? null : ted.getData();
            if (data == null) {
                assert false : "key data error: " + item + " -> " + base64;
                continue;
            }
            //
            //  3. got data with target (ID + terminal)
            //
            data = result.put(target, data);
            assert data == null : "duplicated terminal: " + item + ", " + keys;
        }
        // OK
        return result;
    }

}
