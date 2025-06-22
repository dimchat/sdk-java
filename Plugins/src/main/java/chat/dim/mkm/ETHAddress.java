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

import chat.dim.digest.Keccak256;
import chat.dim.format.Hex;
import chat.dim.protocol.Address;
import chat.dim.protocol.EntityType;
import chat.dim.type.ConstantString;

/**
 *  Address like Ethereum
 *
 *  <blockquote><pre>
 *  data format:
 *      "0x{address}"
 *
 *  algorithm:
 *      fingerprint = PK.data;
 *      digest      = keccak256(fingerprint);
 *      address     = hex_encode(digest.suffix(20));
 *  </pre></blockquote>
 */
public final class ETHAddress extends ConstantString implements Address {

    public ETHAddress(String string) {
        super(string);
    }

    @Override
    public int getNetwork() {
        return EntityType.USER.value;
    }

    // https://eips.ethereum.org/EIPS/eip-55
    private static String eip55(String hex) {
        StringBuilder sb = new StringBuilder();
        byte[] hash = Keccak256.digest(hex.getBytes());
        char ch;
        for (int i = 0; i < 40; ++i) {
            ch = hex.charAt(i);
            if (ch > '9') {
                // check for each 4 bits in the hash table
                // if the first bit is '1',
                //     change the character to uppercase
                ch -= (hash[i >> 1] << (i << 2 & 4) & 0x80) >> 2;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static boolean isETH(String address) {
        if (address.length() != 42) {
            return false;
        }
        if (address.charAt(0) != '0' || address.charAt(1) != 'x') {
            return false;
        }
        char ch;
        for (int i = 2; i < 42; ++i) {
            ch = address.charAt(i);
            if (ch >= '0' && ch <= '9') {
                continue;
            }
            if (ch >= 'A' && ch <= 'Z') {
                continue;
            }
            if (ch >= 'a' && ch <= 'z') {
                continue;
            }
            // unexpected character
            return false;
        }
        return true;
    }

    public static String getValidateAddress(String address) {
        if (!isETH(address)) {
            // not an ETH address
            return null;
        }
        String lower = address.substring(2).toLowerCase();
        return "0x" + eip55(lower);
    }

    public static boolean isValidate(String address) {
        String validate = getValidateAddress(address);
        return validate != null && validate.equals(address);
    }

    /**
     *  Generate ETH address with key.data
     *
     * @param fingerprint = key.data
     * @return Address object
     */
    public static ETHAddress generate(byte[] fingerprint) {
        if (fingerprint.length == 65) {
            // skip first char
            byte[] data = new byte[64];
            System.arraycopy(fingerprint, 1, data, 0, 64);
            fingerprint = data;
        }
        assert fingerprint.length == 64 : "key data length error: " + fingerprint.length;
        // 1. digest = keccak256(fingerprint);
        byte[] digest = Keccak256.digest(fingerprint);
        // 2. address = hex_encode(digest.suffix(20));
        byte[] tail = new byte[20];
        System.arraycopy(digest, digest.length - 20, tail, 0, 20);
        String address = "0x" + eip55(Hex.encode(tail));
        return new ETHAddress(address);
    }

    /**
     *  Parse a string for ETH address
     *
     * @param address - address string
     * @return null on error
     */
    public static ETHAddress parse(String address) {
        if (!isETH(address)) {
            // not an ETH address
            return null;
        }
        return new ETHAddress(address);
    }
}
