
#include "chat_dim_ecc_Secp256k1.h"

#include "micro-ecc/uECC.h"
#include "der.h"

/*
 * Class:     chat_dim_ecc_Secp256k1
 * Method:    secp256k1MakeKeys
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_chat_dim_ecc_Secp256k1_makeKeys
    (JNIEnv *env, jclass cls) {

    uint8_t keys[96] = {0};
    uint8_t *pubkey = keys;
    uint8_t *prikey = keys + 64;
    int res = uECC_make_key(pubkey, prikey, uECC_secp256k1());
    if (res != 1) {
        return NULL;
    }

    jbyteArray key_pair = (*env)->NewByteArray(env, 96);
    (*env)->SetByteArrayRegion(env, key_pair, 0, 96, (jbyte *)keys);
    return key_pair;
}

/*
 * Class:     chat_dim_ecc_Secp256k1
 * Method:    secp256k1ComputePublicKey
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_chat_dim_ecc_Secp256k1_computePublicKey
    (JNIEnv *env, jclass cls, jbyteArray priKey) {

    long len;
    len = (*env)->GetArrayLength(env, priKey);
    const uint8_t private_key[len];
    (*env)->GetByteArrayRegion(env, priKey, 0, (jsize)len, (jbyte *)private_key);
    (*env)->DeleteLocalRef(env, priKey);

    uint8_t public_key[64] = {0};
    int res = uECC_compute_public_key(private_key, public_key, uECC_secp256k1());
    if (res != 1) {
        return NULL;
    }

    jbyteArray pubkey = (*env)->NewByteArray(env, 64);
    (*env)->SetByteArrayRegion(env, pubkey, 0, 64, (jbyte *)public_key);
    return pubkey;
}

/*
 * Class:     chat_dim_ecc_Secp256k1
 * Method:    secp256k1Verify
 * Signature: ([B[B[B)I
 */
JNIEXPORT jint JNICALL Java_chat_dim_ecc_Secp256k1_verify
    (JNIEnv *env, jclass cls, jbyteArray pubKey, jbyteArray msgHash, jbyteArray sig) {

    long len;
    len = (*env)->GetArrayLength(env, pubKey);
    const uint8_t public_key[len];
    (*env)->GetByteArrayRegion(env, pubKey, 0, (jsize)len, (jbyte *)public_key);
    (*env)->DeleteLocalRef(env, pubKey);

    long hashSize = (*env)->GetArrayLength(env, msgHash);
    const uint8_t message_hash[len];
    (*env)->GetByteArrayRegion(env, msgHash, 0, (jsize)hashSize, (jbyte *)message_hash);
    (*env)->DeleteLocalRef(env, msgHash);

    len = (*env)->GetArrayLength(env, sig);
    const uint8_t signature[len];
    (*env)->GetByteArrayRegion(env, sig, 0, (jsize)len, (jbyte *)signature);
    (*env)->DeleteLocalRef(env, sig);

    uint8_t sig_der[64];
    int res = ecc_der_to_sig(signature, (int)len, sig_der);
    if (res != 0) {
        return -1;
    }

    return uECC_verify(public_key, message_hash, (unsigned)hashSize, sig_der, uECC_secp256k1());
}

/*
 * Class:     chat_dim_ecc_Secp256k1
 * Method:    secp256k1Sign
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_chat_dim_ecc_Secp256k1_sign
    (JNIEnv *env, jclass cls, jbyteArray priKey, jbyteArray msgHash) {

    long len;
    len = (*env)->GetArrayLength(env, priKey);
    const uint8_t private_key[len];
    (*env)->GetByteArrayRegion(env, priKey, 0, (jsize)len, (jbyte *)private_key);
    (*env)->DeleteLocalRef(env, priKey);

    long hashSize = (*env)->GetArrayLength(env, msgHash);
    const uint8_t message_hash[len];
    (*env)->GetByteArrayRegion(env, msgHash, 0, (jsize)len, (jbyte *)message_hash);
    (*env)->DeleteLocalRef(env, msgHash);

    uint8_t sig[64];
    int res = uECC_sign(private_key, message_hash, (unsigned)hashSize, sig, uECC_secp256k1());
    if (res != 1) {
        return NULL;
    }

    uint8_t vchSig[72];
    int nSigLen = ecc_sig_to_der(sig, vchSig);

    jbyteArray signature = (*env)->NewByteArray(env, nSigLen);
    (*env)->SetByteArrayRegion(env, signature, 0, nSigLen, (jbyte *)vchSig);
    return signature;
}
