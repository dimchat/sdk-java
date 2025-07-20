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
package chat.dim.format;

import java.net.URI;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.type.Dictionary;

/**
 *  PNF - Portable Network File
 */
public final class BaseNetworkFile extends Dictionary implements PortableNetworkFile {

    private final BaseFileWrapper wrapper;

    public BaseNetworkFile(Map<String, Object> dictionary) {
        super(dictionary);
        wrapper = new BaseFileWrapper(toMap());
    }

    public BaseNetworkFile(TransportableData data, String filename, URI url, DecryptKey key) {
        super();
        wrapper = new BaseFileWrapper(toMap());
        // file data
        if (data != null) {
            wrapper.setData(data);
        }
        // file name
        if (filename != null) {
            wrapper.setFilename(filename);
        }
        // download URL
        if (url != null) {
            wrapper.setURL(url);
        }
        // decrypt key
        if (key != null) {
            wrapper.setPassword(key);
        }
    }

    /**
     *  file data
     */

    @Override
    public byte[] getData() {
        TransportableData ted = wrapper.getData();
        return ted == null ? null : ted.getData();
    }

    @Override
    public void setData(byte[] binary) {
        wrapper.setData(binary);
    }

    /**
     *  file name
     */

    @Override
    public String getFilename() {
        return wrapper.getFilename();
    }

    @Override
    public void setFilename(String name) {
        wrapper.setFilename(name);
    }

    /**
     *  download URL
     */

    @Override
    public URI getURL() {
        return wrapper.getURL();
    }

    @Override
    public void setURL(URI url) {
        wrapper.setURL(url);
    }

    /**
     *  decrypt key
     */

    @Override
    public DecryptKey getPassword() {
        return wrapper.getPassword();
    }

    @Override
    public void setPassword(DecryptKey key) {
        wrapper.setPassword(key);
    }

    /**
     *  encoding
     */

    @Override
    public String toString() {
        String urlString = getURLString();
        if (urlString != null) {
            // only contains 'URL', return the URL string directly
            return urlString;
        }
        // not a single URL, encode the entire dictionary
        return JSONMap.encode(toMap());
    }

    @Override
    public Object toObject() {
        String urlString = getURLString();
        if (urlString != null) {
            // only contains 'URL', return the URL string directly
            return urlString;
        }
        // not a single URL, return the entire dictionary
        return toMap();
    }

    private String getURLString() {
        String urlString = getString("URL");
        if (urlString == null) {
            return null;
        } else if (urlString.startsWith("data:")) {
            // 'data:...;...,...'
            return urlString;
        }
        int count = size();
        if (count == 1) {
            // if only contains 'URL' field, return the URL string directly
            return urlString;
        } else if (count == 2 && containsKey("filename")) {
            // ignore 'filename' field
            return urlString;
        } else {
            // not a single URL
            return null;
        }
    }

}
