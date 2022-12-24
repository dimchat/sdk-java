/**
 *  Refs:
 *      https://github.com/kmackay/micro-ecc
 *      https://github.com/digitalbitbox/mcu/blob/master/src/ecc.c
 */

#include <string.h>

static int trim_to_32_bytes(const uint8_t *src, int src_len, uint8_t *dst)
{
    int dst_offset;
    while (*src == '\0' && src_len > 0) {
        src++;
        src_len--;
    }
    if (src_len > 32 || src_len < 1) {
        return 1;
    }
    dst_offset = 32 - src_len;
    memset(dst, 0, dst_offset);
    memcpy(dst + dst_offset, src, src_len);
    return 0;
}

static inline int ecc_der_to_sig(const uint8_t *der, int der_len, uint8_t *sig_64)
{
    /*
     * Structure is:
     *   0x30 0xNN  SEQUENCE + s_length
     *   0x02 0xNN  INTEGER + r_length
     *   0xAA 0xBB  ..   r_length bytes of "r" (offset 4)
     *   0x02 0xNN  INTEGER + s_length
     *   0xMM 0xNN  ..   s_length bytes of "s" (offset 6 + r_len)
     */
    int seq_len;
    //uint8_t r_bytes[32];
    //uint8_t s_bytes[32];
    int r_len;
    int s_len;

    //memset(r_bytes, 0, sizeof(r_bytes));
    //memset(s_bytes, 0, sizeof(s_bytes));

    /*
     * Must have at least:
     * 2 bytes sequence header and length
     * 2 bytes R integer header and length
     * 1 byte of R
     * 2 bytes S integer header and length
     * 1 byte of S
     *
     * 8 bytes total
     */
    if (der_len < 8 || der[0] != 0x30 || der[2] != 0x02) {
        return 1;
    }

    seq_len = der[1];
    if ((seq_len <= 0) || (seq_len + 2 != der_len)) {
        return 1;
    }

    r_len = der[3];
    /*
     * Must have at least:
     * 2 bytes for R header and length
     * 2 bytes S integer header and length
     * 1 byte of S
     */
    if ((r_len < 1) || (r_len > seq_len - 5) || (der[4 + r_len] != 0x02)) {
        return 1;
    }
    s_len = der[5 + r_len];

    /**
     * Must have:
     * 2 bytes for R header and length
     * r_len bytes for R
     * 2 bytes S integer header and length
     */
    if ((s_len < 1) || (s_len != seq_len - 4 - r_len)) {
        return 1;
    }

    /*
     * ASN.1 encoded integers are zero-padded for positive integers. Make sure we have
     * a correctly-sized buffer and that the resulting integer isn't too large.
     */
    if (trim_to_32_bytes(&der[4], r_len, sig_64) ||
            trim_to_32_bytes(&der[6 + r_len], s_len, sig_64 + 32)) {
        return 1;
    }

    return 0;
}

static inline int ecc_sig_to_der(const uint8_t *sig, uint8_t *der)
{
    int i;
    uint8_t *p = der, *len, *len1, *len2;
    *p = 0x30;
    p++; // sequence
    *p = 0x00;
    len = p;
    p++; // len(sequence)

    *p = 0x02;
    p++; // integer
    *p = 0x00;
    len1 = p;
    p++; // len(integer)

    // process R
    i = 0;
    while (sig[i] == 0 && i < 32) {
        i++; // skip leading zeroes
    }
    if (sig[i] >= 0x80) { // put zero in output if MSB set
        *p = 0x00;
        p++;
        *len1 = *len1 + 1;
    }
    while (i < 32) { // copy bytes to output
        *p = sig[i];
        p++;
        *len1 = *len1 + 1;
        i++;
    }

    *p = 0x02;
    p++; // integer
    *p = 0x00;
    len2 = p;
    p++; // len(integer)

    // process S
    i = 32;
    while (sig[i] == 0 && i < 64) {
        i++; // skip leading zeroes
    }
    if (sig[i] >= 0x80) { // put zero in output if MSB set
        *p = 0x00;
        p++;
        *len2 = *len2 + 1;
    }
    while (i < 64) { // copy bytes to output
        *p = sig[i];
        p++;
        *len2 = *len2 + 1;
        i++;
    }

    *len = *len1 + *len2 + 4;
    return *len + 2;
}
