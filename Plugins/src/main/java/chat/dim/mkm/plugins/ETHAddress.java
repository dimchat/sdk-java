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
package chat.dim.mkm.plugins;

import chat.dim.digest.Keccak256;
import chat.dim.format.Hex;
import chat.dim.protocol.Address;
import chat.dim.protocol.NetworkType;

/**
 *  Address like Ethereum
 *
 *      data format: "0x{address}"
 *
 *      algorithm:
 *          fingerprint = sign(seed, SK);  // public key data
 *          digest      = keccak256(fingerprint);
 *          address     = hex_encode(digest.suffix(20));
 */
public final class ETHAddress extends chat.dim.type.String implements Address {

    public ETHAddress(String string) {
        super(string);
    }

    @Override
    public byte getNetwork() {
        return NetworkType.Main.value;
    }

    /**
     *  Generate address with fingerprint and network ID
     *
     * @param fingerprint = key.data
     * @return Address object
     */
    public static ETHAddress generate(byte[] fingerprint) {
        // 1. digest = keccak256(fingerprint);
        byte[] digest = Keccak256.digest(fingerprint);
        // 2. address = hex_encode(digest.suffix(20));
        byte[] tail = new byte[20];
        System.arraycopy(digest, digest.length - 20, tail, 0, 20);
        String address = "0x" + Hex.encode(tail);
        return new ETHAddress(address);
    }

    /**
     *  Parse a string for ETH address
     *
     * @param string - address string
     * @return null on error
     */
    public static ETHAddress parse(String string) {
        // decode
        if (string.startsWith("0x")) {
            string = string.substring(2);
        }
        byte[] data = Hex.decode(string);
        if (data.length != 20) {
            throw new IndexOutOfBoundsException("address length error: " + data.length);
        }
        return new ETHAddress(string);
    }
}
