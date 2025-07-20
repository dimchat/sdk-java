/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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
package chat.dim.plugins;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.format.EncodeAlgorithms;
import chat.dim.format.JSONMap;
import chat.dim.format.PortableNetworkFile;
import chat.dim.format.TransportableData;
import chat.dim.type.Converter;
import chat.dim.type.Mapper;

/**
 *  Format GeneralFactory
 */
public class FormatGeneralFactory implements GeneralFormatHelper,
                                             PortableNetworkFileHelper,
                                             TransportableDataHelper {

    private final Map<String, TransportableData.Factory> tedFactories = new HashMap<>();

    private PortableNetworkFile.Factory pnfFactory = null;

    /**
     *  Split text string to array:
     *  <blockquote>
     *      ["{TEXT}", "{algorithm}", "{content-type}"]
     *  </blockquote>
     */
    public static List<String> split(String text) {
        List<String> array = new ArrayList<>();
        // "{TEXT}", or
        // "base64,{BASE64_ENCODE}", or
        // "data:image/png;base64,{BASE64_ENCODE}"
        int pos1 = text.indexOf("://");
        if (pos1 > 0) {
            // [URL]
            array.add(text);
            return array;
        } else {
            // skip 'data:'
            pos1 = text.indexOf(':') + 1;
        }
        // seeking for 'content-type'
        int pos2 = text.indexOf(';', pos1);
        if (pos2 > pos1) {
            array.add(text.substring(pos1, pos2));
            pos1 = pos2 + 1;
        }
        // seeking for 'algorithm'
        pos2 = text.indexOf(',', pos1);
        if (pos2 > pos1) {
            array.add(0, text.substring(pos1, pos2));
            pos1 = pos2 + 1;
        }
        if (pos1 == 0) {
            // [data]
            array.add(0, text);
        } else {
            // [data, algorithm, type]
            array.add(0, text.substring(pos1));
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> decode(Object data, String defaultKey) {
        if (data instanceof Mapper) {
            return ((Mapper) data).toMap();
        } else if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        String text = data instanceof String ? (String) data : data.toString();
        if (text.isEmpty()) {
            return null;
        } else if (text.startsWith("{") && text.endsWith("}")) {
            return JSONMap.decode(text);
        }
        Map<String, Object> info = new HashMap<>();
        List<String> array = split(text);
        int size = array.size();
        if (size == 1) {
            info.put(defaultKey, array.get(0));
        } else {
            assert size > 1 : "split error: " + text + " => " + array;
            info.put("data", array.get(0));
            info.put("algorithm", array.get(1));
            if (size > 2) {
                // 'data:...;...,...'
                info.put("content-type", array.get(2));
                if (text.startsWith("data:")) {
                    info.put("URL", text);
                }
            }
        }
        return info;
    }

    @Override
    public String getFormatAlgorithm(Map<?, ?> ted, String defaultValue) {
        return Converter.getString(ted.get("algorithm"), defaultValue);
    }

    ///
    ///   TED - Transportable Encoded Data
    ///

    @Override
    public void setTransportableDataFactory(String algorithm, TransportableData.Factory factory) {
        tedFactories.put(algorithm, factory);
    }

    @Override
    public TransportableData.Factory getTransportableDataFactory(String algorithm) {
        return tedFactories.get(algorithm);
    }

    @Override
    public TransportableData createTransportableData(byte[] data, String algorithm) {
        if (algorithm == null) {
            algorithm = EncodeAlgorithms.DEFAULT;
        }
        TransportableData.Factory factory = getTransportableDataFactory(algorithm);
        assert factory != null : "TED algorithm not support: " + algorithm;
        return factory.createTransportableData(data);
    }

    @Override
    public TransportableData parseTransportableData(Object ted) {
        if (ted == null) {
            return null;
        } else if (ted instanceof TransportableData) {
            return (TransportableData) ted;
        }
        // unwrap
        Map<String, Object> info = decode(ted, "data");
        if (info == null) {
            //assert false : "TED error: " + ted;
            return null;
        }
        String algo = getFormatAlgorithm(info, null);
        // assert algo != null : "TED error: " + ted;
        TransportableData.Factory factory = algo == null ? null : getTransportableDataFactory(algo);
        if (factory == null) {
            // unknown algorithm, get default factory
            factory = getTransportableDataFactory("*");  // unknown
            if (factory == null) {
                assert false : "default TED factory not found: " + ted;
                return null;
            }
        }
        return factory.parseTransportableData(info);
    }

    ///
    ///   PNF - Portable Network File
    ///

    @Override
    public void setPortableNetworkFileFactory(PortableNetworkFile.Factory factory) {
        pnfFactory = factory;
    }

    @Override
    public PortableNetworkFile.Factory getPortableNetworkFileFactory() {
        return pnfFactory;
    }

    @Override
    public PortableNetworkFile createPortableNetworkFile(TransportableData data, String filename,
                                                         URI url, DecryptKey password) {
        PortableNetworkFile.Factory factory = getPortableNetworkFileFactory();
        assert factory != null : "PNF factory not ready";
        return factory.createPortableNetworkFile(data, filename, url, password);
    }

    @Override
    public PortableNetworkFile parsePortableNetworkFile(Object pnf) {
        if (pnf == null) {
            return null;
        } else if (pnf instanceof PortableNetworkFile) {
            return (PortableNetworkFile) pnf;
        }
        // unwrap
        Map<String, Object> info = decode(pnf, "URL");
        if (info == null) {
            //assert false : "PNF error: " + pnf;
            return null;
        }
        PortableNetworkFile.Factory factory = getPortableNetworkFileFactory();
        assert factory != null : "PNF factory not ready";
        return factory.parsePortableNetworkFile(info);
    }

}
