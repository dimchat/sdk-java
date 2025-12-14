/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.core;

import java.util.List;

import chat.dim.protocol.Document;
import chat.dim.protocol.EncryptKey;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.VerifyKey;

public interface Archivist {

    /**
     *  Save meta for entity ID (must verify first)
     *
     * @param did  - entity ID
     * @param meta - entity meta
     * @return true on success
     */
    boolean saveMeta(Meta meta, ID did);

    /**
     *  Save entity document with ID (must verify first)
     *
     * @param did  - entity ID
     * @param doc - entity document
     * @return true on success
     */
    boolean saveDocument(Document doc, ID did);

    //
    //  Public Keys
    //

    /**
     *  Get meta.key
     *
     * @param did - entity ID
     * @return null on not found
     */
    VerifyKey getMetaKey(ID did);

    /**
     *  Get visa.key
     *
     * @param did - entity ID
     * @return null on not found
     */
    EncryptKey getVisaKey(ID did);

    //
    //  Local Users
    //

    /**
     *  Get all local users (for decrypting received message)
     *
     * @return users with private key
     */
    List<ID> getLocalUsers();

}
