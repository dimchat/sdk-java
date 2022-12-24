/* license: https://mit-license.org
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
package chat.dim.ecc;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECField;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;

import chat.dim.utils.CryptoUtils;

final class ECCGenerator {

    public static ECPublicKey getPublicKey(ECPrivateKey pk) throws GeneralSecurityException {
        ECParameterSpec params = pk.getParams();
        ECPoint w = scalmult(params.getCurve(), pk.getParams().getGenerator(), pk.getS());
        KeyFactory kg = CryptoUtils.getKeyFactory(CryptoUtils.EC);
        return (ECPublicKey) kg.generatePublic(new ECPublicKeySpec(w, params));
    }

    private static ECPoint scalmult(EllipticCurve curve, ECPoint g, BigInteger kin) {
        ECField field = curve.getField();
        if (!(field instanceof ECFieldFp)) throw new UnsupportedOperationException(field.getClass().getCanonicalName());
        BigInteger p = ((ECFieldFp) field).getP();
        BigInteger a = curve.getA();
        ECPoint R = ECPoint.POINT_INFINITY;
        // value only valid for curve secp256k1, code taken from https://www.secg.org/sec2-v2.pdf,
        // see "Finally the order n of G and the cofactor are: n = "FF.."
        BigInteger SECP256K1_Q = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141",16);
        BigInteger k = kin.mod(SECP256K1_Q); // uses this !
        // wrong as per comment from President James Moveon Polk
        // BigInteger k = kin.mod(p); // do not use this !
        int length = k.bitLength();
        byte[] binarray = new byte[length];
        for (int i = 0; i <= length - 1; i++) {
            binarray[i] = k.mod(FieldP_2).byteValue();
            k = k.shiftRight(1);
        }
        for (int i = length - 1; i >= 0; i--) {
            R = doublePoint(p, a, R);
            if (binarray[i] == 1) R = addPoint(p, a, R, g);
        }
        return R;
    }

    private static ECPoint addPoint(BigInteger p, BigInteger a, ECPoint r, ECPoint g) {
        if (r.equals(ECPoint.POINT_INFINITY)) return g;
        if (g.equals(ECPoint.POINT_INFINITY)) return r;
        if (r == g || r.equals(g)) return doublePoint(p, a, r);
        BigInteger gX = g.getAffineX();
        BigInteger sY = g.getAffineY();
        BigInteger rX = r.getAffineX();
        BigInteger rY = r.getAffineY();
        BigInteger slope = (rY.subtract(sY)).multiply(rX.subtract(gX).modInverse(p)).mod(p);
        BigInteger Xout = (slope.modPow(FieldP_2, p).subtract(rX)).subtract(gX).mod(p);
        BigInteger Yout = sY.negate().mod(p);
        Yout = Yout.add(slope.multiply(gX.subtract(Xout))).mod(p);
        return new ECPoint(Xout, Yout);
    }

    private static ECPoint doublePoint(BigInteger p, BigInteger a, ECPoint R) {
        if (R.equals(ECPoint.POINT_INFINITY)) return R;
        BigInteger slope = (R.getAffineX().pow(2)).multiply(FieldP_3);
        slope = slope.add(a);
        slope = slope.multiply((R.getAffineY().multiply(FieldP_2)).modInverse(p));
        BigInteger Xout = slope.pow(2).subtract(R.getAffineX().multiply(FieldP_2)).mod(p);
        BigInteger Yout = (R.getAffineY().negate()).add(slope.multiply(R.getAffineX().subtract(Xout))).mod(p);
        return new ECPoint(Xout, Yout);
    }

    static private final BigInteger FieldP_2 = BigInteger.valueOf(2); // constant for scalar operations
    static private final BigInteger FieldP_3 = BigInteger.valueOf(3); // constant for scalar operations
}
