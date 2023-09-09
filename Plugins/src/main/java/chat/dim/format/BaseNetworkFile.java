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
import chat.dim.crypto.SymmetricKey;
import chat.dim.type.Dictionary;

public class BaseNetworkFile extends Dictionary implements PortableNetworkFile {

    private TransportableData attachment; // file content (not encrypted)
    private DecryptKey password;          // key to decrypt data

    public BaseNetworkFile(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy load
        attachment = null;
        password = null;
    }

    public BaseNetworkFile(URI url, byte[] data, String filename, DecryptKey key) {
        super();
        // remote URL
        setURL(url);
        // file data (lazy encode)
        attachment = TransportableData.create(data);
        // file name
        setFilename(filename);
        // decrypt key
        setPassword(key);
    }

    @Override
    public void setURL(URI url) {
        if (url == null) {
            remove("URL");
        } else {
            put("URL", url.toString());
        }
    }

    @Override
    public URI getURL() {
        String url = getString("URL", null);
        return url == null ? null : URI.create(url);
    }

    @Override
    public void setData(byte[] data) {
        if (data != null && data.length > 0) {
            attachment = TransportableData.create(data);
            // lazy encode
            // put("data", attachment.toObject());
        } else {
            attachment = null;
            remove("data");
        }
    }

    @Override
    public byte[] getData() {
        TransportableData ted = attachment;
        if (ted == null) {
            String base64 = getString("data", null);
            attachment = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
    }

    @Override
    public void setFilename(String filename) {
        if (filename == null) {
            remove("filename");
        } else {
            put("filename", filename);
        }
    }

    @Override
    public String getFilename() {
        return getString("filename", null);
    }

    @Override
    public void setPassword(DecryptKey key) {
        setMap("key", key);
        password = key;
    }

    @Override
    public DecryptKey getPassword() {
        if (password == null) {
            Object info = get("key");
            password = SymmetricKey.parse(info);
        }
        return password;
    }

    private boolean dataExists() {
        if (get("data") != null) {
            // data encoded already
            return true;
        }
        TransportableData ted = attachment;
        if (ted != null) {
            // encode data string
            put("data", ted.toObject());
            return true;
        }
        // data not exists
        return false;
    }

    @Override
    public String toString() {
        // check 'data' and 'key'
        if (dataExists() || getPassword() != null) {
            // not a single URL, encode the entire dictionary
            return JSONMap.encode(toMap());
        }
        // field 'data' not exists, means this file was uploaded onto a CDN,
        // if 'key' not exists too, just return 'URL' string here.
        assert get("filename") == null: "PNF error: " + toMap();
        String url = getString("URL", null);
        assert url != null : "URL cannot be empty: " + toMap();
        return url;
    }

    @Override
    public Object toObject() {
        return toString();
    }
}
