/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
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

import java.util.Arrays;

import chat.dim.digest.RIPEMD160;
import chat.dim.digest.SHA256;
import chat.dim.format.Base58;
import chat.dim.protocol.Address;
import chat.dim.type.ConstantString;

/**
 *  Address like BitCoin
 *
 *  <blockquote><pre>
 *  data format: "network+digest+code"
 *      network    --  1 byte
 *      digest     -- 20 bytes
 *      check code --  4 bytes
 *
 *  algorithm:
 *      fingerprint = PK.data
 *      digest      = ripemd160(sha256(fingerprint));
 *      code        = sha256(sha256(network + digest)).prefix(4);
 *      address     = base58_encode(network + digest + code);
 *  </pre></blockquote>
 */
public final class BTCAddress extends ConstantString implements Address {

    private final byte type;

    public BTCAddress(String string, byte network) {
        super(string);
        type = network;
    }

    @Override
    public int getNetwork() {
        return type;
    }

    /**
     *  Generate BTC address with fingerprint and network ID
     *
     * @param fingerprint - meta.fingerprint or key.data
     * @param network - address type
     * @return Address object
     */
    public static BTCAddress generate(byte[] fingerprint, byte network) {
        // 1. digest = ripemd160(sha256(fingerprint))
        byte[] digest = RIPEMD160.digest(SHA256.digest(fingerprint));
        // 2. head = network + digest
        byte[] head = new byte[21];
        head[0] = network;
        System.arraycopy(digest, 0, head, 1, 20);
        // 3. cc = sha256(sha256(head)).prefix(4)
        byte[] cc = checkCode(head);
        // 4. data = base58_encode(head + cc)
        byte[] data = new byte[25];
        System.arraycopy(head, 0, data, 0, 21);
        System.arraycopy(cc,0, data, 21, 4);
        return new BTCAddress(Base58.encode(data), network);
    }

    /**
     *  Parse a string for BTC address
     *
     * @param address - address string
     * @return null on error
     */
    public static BTCAddress parse(String address) {
        int len = address.length();
        if (len < 26 || len > 35) {
            return null;
        }
        // decode
        byte[] data = Base58.decode(address);
        if (data == null || data.length != 25) {
            return null;
        }
        // Check Code
        byte[] prefix = new byte[21];
        byte[] suffix = new byte[4];
        System.arraycopy(data, 0, prefix, 0, 21);
        System.arraycopy(data, 21, suffix, 0, 4);
        byte[] cc = checkCode(prefix);
        if (Arrays.equals(cc, suffix)) {
            return new BTCAddress(address, data[0]);
        } else {
            return null;
        }
    }

    private static byte[] checkCode(byte[] data) {
        byte[] sha256d = SHA256.digest(SHA256.digest(data));
        assert sha256d != null : "sha256 error";
        byte[] cc = new byte[4];
        System.arraycopy(sha256d, 0, cc, 0, 4);
        return cc;
    }
}
