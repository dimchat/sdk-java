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

import java.util.HashMap;
import java.util.Map;

import chat.dim.protocol.ID;


public class UserEncryptedBundle implements EncryptedBundle {

    // terminal -> encrypted key.data
    private final Map<String, byte[]> map = new HashMap<>();

    public UserEncryptedBundle() {
        super();
    }

    @Override
    public String toString() {
        String clazz = getClass().getSimpleName();
        StringBuilder sb = new StringBuilder();
        String target;
        byte[] data;
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            target = entry.getKey();
            data = entry.getValue();
            if (target == null || data == null) {
                assert false : "entry error: " + entry;
                continue;
            }
            sb.append("\t\"").append(target).append("\": ").append(data.length).append(" byte(s)\n");
        }
        return "<" + clazz + " count=" + map.size() + ">\n" + sb + "</" + clazz + ">";
    }

    @Override
    public Map<String, byte[]> toMap() {
        return map;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public byte[] put(String terminal, byte[] data) {
        if (data == null) {
            return map.remove(terminal);
        } else {
            return map.put(terminal, data);
        }
    }

    @Override
    public byte[] remove(String terminal) {
        return map.remove(terminal);
    }

    @Override
    public byte[] get(String terminal) {
        return map.get(terminal);
    }

    @Override
    public Map<String, Object> encode(ID did) {
        return SharedVisaAgent.helper.encodeBundle(this, did);
    }

}
