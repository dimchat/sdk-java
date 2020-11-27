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

import java.util.Map;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;

import chat.dim.Entity;
import chat.dim.EntityParser;
import chat.dim.mkm.plugins.BTCAddress;
import chat.dim.mkm.plugins.BTCMeta;
import chat.dim.mkm.plugins.DefaultMeta;
import chat.dim.mkm.plugins.ETHAddress;
import chat.dim.mkm.plugins.ETHMeta;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.Profile;

public abstract class Plugins extends chat.dim.crypto.Plugins {

    static {

        /*
         *  Register classes
         */
        // JsON
        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        serializeConfig.put(Address.class, ToStringSerializer.instance);
        serializeConfig.put(ID.class, ToStringSerializer.instance);

        Entity.parser = new EntityParser() {

            @Override
            protected Address createAddress(String string) {
                if (string == null) {
                    return null;
                }
                Address address = super.createAddress(string);
                if (address != null) {
                    return address;
                }
                int len = string.length();
                if (len == 42) {
                    return ETHAddress.parse(string);
                }
                return BTCAddress.parse(string);
            }

            @Override
            protected Meta createMeta(Map<String, Object> meta) {
                int version = (int) meta.get("version");
                if (MetaType.Default.equals(version)) {
                    return new DefaultMeta(meta);
                } else if (MetaType.BTC.equals(version) || MetaType.ExBTC.equals(version)) {
                    return new BTCMeta(meta);
                } else if (MetaType.ETH.equals(version) || MetaType.ExETH.equals(version)) {
                    return new ETHMeta(meta);
                }
                return null;
            }

            @Override
            protected Profile createProfile(Map<String, Object> profile) {
                ID identifier = parseID(profile.get("ID"));
                if (identifier == null) {
                    return null;
                }
                if (NetworkType.isUser(identifier.getType())) {
                    return new UserProfile(profile);
                } else if (NetworkType.isGroup(identifier.getType())) {
                    return new BaseBulletin(profile);
                }
                return new BaseProfile(profile);
            }
        };
    }
}
