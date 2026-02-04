/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2026 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Albert Moky
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import chat.dim.format.Base64;
import chat.dim.mkm.Identifier;
import chat.dim.protocol.ID;
import chat.dim.protocol.TransportableData;


public class DefaultBundleHelper implements EncryptedBundleHelper {

    @Override
    public Map<String, Object> encodeBundle(EncryptedBundle bundle, ID did) {
        assert did.getTerminal() == null : "ID should not contain terminal here: " + did;
        String identifier = Identifier.concat(did.getName(), did.getAddress(), null);
        Map<String, Object> encodedKeys = new HashMap<>();
        String target;
        byte[] data;
        String base64;
        Map<String, byte[]> map = bundle.toMap();
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            target = entry.getKey();
            data = entry.getValue();
            // encode data
            base64 = Base64.encode(data);
            assert base64 != null : "failed to encode data: " + Arrays.toString(data);
            if (target.isEmpty() || target.equals("*")) {
                target = identifier;
            } else {
                target = identifier + "/" + target;
            }
            // insert to 'message.keys' with ID + terminal
            encodedKeys.put(target, base64);
        }
        // OK
        return encodedKeys;
    }

    @Override
    public EncryptedBundle decodeBundle(Map<String, Object> encodedKeys, ID did, Iterable<String> terminals) {
        EncryptedBundle bundle = new UserEncryptedBundle();
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
                base64 = encodedKeys.get(identifier);
            } else {
                base64 = encodedKeys.get(identifier + "/" + target);
            }
            if (base64 == null) {
                // key data not found
                continue;
            }
            //
            //  2. decode data
            //
            ted = TransportableData.parse(base64);
            data = ted == null ? null : ted.getBytes();
            if (data == null || data.length == 0) {
                assert false : "key data error: " + item + " -> " + base64;
                continue;
            }
            //
            //  3. put data for target (ID terminal)
            //
            data = bundle.put(target, data);
            assert data == null : "duplicated terminal: " + item + ", " + encodedKeys;
        }
        // OK
        return bundle;
    }

}
