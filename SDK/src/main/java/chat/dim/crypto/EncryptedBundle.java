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

import chat.dim.protocol.ID;


/**
 *  User Encrypted Key Data with Terminals
 */
public interface EncryptedBundle {

    // terminal -> encrypted key.data
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
     *  Encode key data
     *
     * @param did - user ID
     * @return encoded key data with targets (ID + terminals)
     */
    Map<String, Object> encode(ID did);

    /**
     *  Decode key data from 'message.keys'
     *
     * @param encodedKeys - encoded key data with targets (ID + terminals)
     * @param did         - receiver ID
     * @param terminals   - visa terminals
     * @return encrypted key data with targets (ID terminals)
     */
    static EncryptedBundle decode(Map<String, Object> encodedKeys, ID did, Iterable<String> terminals) {
        EncryptedBundleHelper helper = SharedVisaAgent.bundleHelper;
        return helper.decodeBundle(encodedKeys, did, terminals);
    }

}
