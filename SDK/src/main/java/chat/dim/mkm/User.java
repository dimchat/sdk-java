/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
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

import java.util.List;
import java.util.Set;

import chat.dim.dkd.EncryptedBundle;
import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.SignKey;


// -----------------------------------------------------------------------------
//  User Entity (with Visa-based Crypto)
// -----------------------------------------------------------------------------

/**
 * User account interface for secure communication (with Visa terminal support).
 *
 * Extends {@link Entity} with user-specific cryptographic operations, contact management,
 * and Visa-based terminal encryption.
 *
 * Supports core secure communication functions:
 *   1. Verification : Verify message signatures using Meta/Visa public keys
 *   2. Encryption   : Encrypt data for specific user terminals (via EncryptedBundle)
 *   3. Signing      : Generate message signatures (local user only)
 *   4. Decryption   : Decrypt terminal-specific data (local user only)
 */
public interface User extends Entity {

    /**
     * List of contact IDs associated with the user.
     *
     * Represents the user's address book/contacts list in the communication system.
     *
     * @return the list of user contact IDs (empty list if none)
     */
    List<ID> getContacts();

    /**
     * Set of terminal identifiers associated with the user's Visa documents.
     *
     * Terminals represent different devices/sessions the user is logged into (e.g., "mobile", "desktop").
     * Retrieved via {@link chat.dim.dkd.VisaAgent#getTerminals(List)} from the user's Visa documents.
     *
     * @return the set of unique terminal identifiers (empty set if none)
     */
    Set<String> getTerminals();

    /**
     * Verifies data and its signature using the user's Meta/Visa public keys.
     *
     * Uses verification keys from {@link chat.dim.dkd.VisaAgent#getVerifyKeys(Meta, List)} to validate message authenticity.
     *
     * @param data is the raw message data to verify
     * @param signature is the digital signature of the data
     * @return true if the signature is valid, false otherwise
     */
    boolean verify(byte[] data, byte[] signature);

    /**
     * Encrypts plaintext data for the user's terminals.
     *
     * Uses {@link chat.dim.dkd.VisaAgent#encryptBundle(byte[], Meta, List)} to create terminal-specific encrypted data:
     * 1. Tries Visa public keys first (terminal-specific encryption)
     * 2. Falls back to Meta public key (wildcard/* encryption)
     *
     * @param plaintext is the raw data to encrypt (usually a symmetric message key)
     * @return an EncryptedBundle with terminal-specific encrypted data
     */
    EncryptedBundle encryptBundle(byte[] plaintext);

    // -------------------------------------------------------------------------
    //  Local User Only Interfaces (Private Key Operations)
    // -------------------------------------------------------------------------

    /**
     * Signs data with the user's private key (local user only).
     *
     * Generates a digital signature for the data using the private key paired with
     * the user's Visa/Meta public key (non-repudiation).
     *
     * @param data is the raw message data to sign
     * @return the digital signature of the data
     */
    byte[] sign(byte[] data);

    /**
     * Decrypts a terminal-specific EncryptedBundle (local user only).
     *
     * Uses private keys from {@link DataSource#getPrivateKeysForDecryption(ID)} to decrypt
     * the bundle, extracting the original plaintext data for the user's terminals.
     *
     * @param bundle is the encrypted data bundle with terminal-specific data
     * @return the decrypted plaintext (null if decryption fails)
     */
    byte[] decryptBundle(EncryptedBundle bundle);

    // -------------------------------------------------------------------------
    //  Visa Document Management
    // -------------------------------------------------------------------------

    /**
     * Signs a Visa document with the user's Meta private key.
     *
     * Uses {@link DataSource#getPrivateKeyForVisaSignature(ID)} to sign the Visa,
     * verifying the document's authenticity (only Meta key is used for Visa signing).
     *
     * @param visa is the visa document to sign
     * @return the signed Visa document (null if signing fails)
     */
    Document signDocument(Document visa);

    /**
     * Verifies the signature of a Visa document.
     *
     * Uses the user's Meta public key (only) to verify the Visa signature,
     * ensuring the document was signed by the user's Meta private key.
     *
     * @param visa is the visa document to verify
     * @return true if the Visa signature is valid, false otherwise
     */
    boolean verifyDocument(Document visa);

    /**
     * Data source interface for user-specific data and cryptographic keys.
     *
     * Extends {@link Entity.DataSource} with user-specific key management, defining the contract
     * for fetching private keys (local user only) and contact information.
     *
     * Core cryptographic responsibilities (Visa/Meta key pairs):
     * 1. Encryption        : Use Visa public key (terminal-specific) or Meta key (fallback)
     * 2. Decryption        : Use private keys paired with Visa/Meta public keys
     * 3. Signing           : Use private key paired with Visa/Meta public key
     * 4. Verification      : Use Visa/Meta public keys
     * 5. Visa Signing      : Use private key paired with Meta public key (only)
     * 6. Visa Verification : Use Meta public key (only)
     */
    interface DataSource extends Entity.DataSource {

        /**
         * Retrieves the contact list for a user.
         *
         * @param user is the unique ID of the target user
         * @return the list of contact IDs (empty list if the user has no contacts)
         */
        List<ID> getContacts(ID user);

        /**
         * Retrieves private keys for decryption (local user only).
         *
         * Returns the private keys paired with the user's Visa/Meta public keys, used to
         * decrypt terminal-specific {@link EncryptedBundle} data.
         *
         * @param user is the unique ID of the target user
         * @return the list of decryption keys (empty list if no keys are available)
         */
        List<DecryptKey> getPrivateKeysForDecryption(ID user);

        /**
         * Retrieves the private key for message signing (local user only).
         *
         * Returns the private key paired with the user's Visa/Meta public key, used to
         * generate digital signatures for messages.
         *
         * @param user is the unique ID of the target user
         * @return the signing key (null if no key is available)
         */
        SignKey getPrivateKeyForSignature(ID user);

        /**
         * Retrieves the private key for Visa signing (local user only).
         *
         * Returns the private key paired with the user's Meta public key (only), used to
         * sign the user's Visa documents (identity verification).
         *
         * @param user is the unique ID of the target user
         * @return the signing key for Visa documents (null if no key is available)
         */
        SignKey getPrivateKeyForVisaSignature(ID user);
    }
}
