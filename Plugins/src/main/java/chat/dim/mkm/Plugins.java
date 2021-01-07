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

import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;

public interface Plugins {

    /*
     *  Address factory
     */
    static void registerAddressFactory() {

        Address.setFactory(new AddressFactory() {
            @Override
            protected Address createAddress(String address) {
                int len = address.length();
                if (len == 42) {
                    return ETHAddress.parse(address);
                }
                return BTCAddress.parse(address);
            }
        });
    }

    /*
     *  Meta factories
     */
    static void registerMetaFactories() {

        Meta.register(MetaType.MKM, new MetaFactory(MetaType.MKM));
        Meta.register(MetaType.BTC, new MetaFactory(MetaType.BTC));
        Meta.register(MetaType.ExBTC, new MetaFactory(MetaType.ExBTC));
        Meta.register(MetaType.ETH, new MetaFactory(MetaType.ETH));
        Meta.register(MetaType.ExETH, new MetaFactory(MetaType.ExETH));
    }

    /*
     *  Document factories
     */
    static void registerDocumentFactories() {

        Document.register("*", new DocumentFactory("*"));
        Document.register(Document.VISA, new DocumentFactory(Document.VISA));
        Document.register(Document.PROFILE, new DocumentFactory(Document.PROFILE));
        Document.register(Document.BULLETIN, new DocumentFactory(Document.BULLETIN));
    }
}
