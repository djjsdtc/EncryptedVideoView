/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cookiework.encryptedvideoview.encryption;

import android.content.Context;
import android.util.Log;

import org.spongycastle.crypto.digests.MD5Digest;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.UrlBase64;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import cookiework.encryptedvideoview.util.DBHelper;
import cookiework.encryptedvideoview.util.ViewerTag;

import static cookiework.encryptedvideoview.Constants.TIME_LOG_TAG;

/**
 * @author Andrew This class it the main workhorse that does all encryption,
 *         decryption and key management. It provides tools to Subscription Processor as
 *         well as being directly accessed by the Firefox extension to encode/decode
 *         messages.
 *         <p>
 *         Message encryption is defined as:
 *         <p>
 *         (message, t) → (ct, t*)
 *         <p>
 *         temp = Sha1-hash[RSA-Signature(message)] k = MD5(temp || 0) ct =
 *         AES_ENCk(message)
 *         <p>
 *         send (ct, t*) to database to recover search on t*
 *         <p>
 *         Message decryption is defined as:
 *         <p>
 *         (ct, t*) → (message, t)
 *         <p>
 *         t* = MD5(temp || 1) k = MD5(temp || 0) message = AES_DECk(ct)
 */
public class PtWittEnc {

    public final DBHelper dbHelper;

    //constructs object nothing special
    public PtWittEnc(Context context) {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        this.dbHelper = new DBHelper(context);
    }

    //Performs an MD5 hash with a 0 appended to the end of the input value.
    //this method is used during the message encryption to generate the K value
    //that is used for the AES encryption
    public byte[] hashMD5(byte[] input) {
        byte[] input2 = appendBytes(input, "0".getBytes());

        MD5Digest md5 = new MD5Digest();
        md5.update(input2, 0, input2.length);

        byte[] digest = new byte[md5.getDigestSize()];
        md5.doFinal(digest, 0);

        return digest;
    }

    //This creates the T* value which is a common step during the encryption
    //decryption and subscription processes it is simply MD5(Temp || 1)
    public String createTStar(byte[] temp) {
        byte[] temp2 = appendBytes(temp, "1".getBytes());

        return new String(UrlBase64.encode(hashMD5(temp2)));
    }

    //helper function
    public byte[] appendBytes(byte[] temp, byte[] zeroOrOne) {
        byte[] temp2 = new byte[temp.length + zeroOrOne.length];
        System.arraycopy(temp, 0, temp2, 0, temp.length);
        System.arraycopy(zeroOrOne, 0, temp2, zeroOrOne.length, zeroOrOne.length);

        return temp2;
    }

    //Decrypts the cipher text into plain text using bouncycastle as the provider
    //and using the AES/ECB/PKCS7Padding cipher instance.
    public String decryptAES(String text, byte[] key) {
        long beginTime = System.currentTimeMillis();

        byte[] cipherText = null;
        try {
            cipherText = UrlBase64.decode(text);
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }

        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        ;
        byte[] pt = null;
        try {
            Cipher ci = Cipher.getInstance("AES/ECB/PKCS7Padding", "SC");
            ci.init(Cipher.DECRYPT_MODE, secretKey);
            pt = ci.doFinal(cipherText);
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }

        long endTime = System.currentTimeMillis();
        Log.i(TIME_LOG_TAG, "decryptAES(): " + (endTime - beginTime));
        return new String(pt);
    }

    //This method decrypts the AES encrypted key with a given tag
    //key=Dec(H1(temp), encKey)
    public String decryptStoredKey(String encKey, String sigma) {
        long beginTime = System.currentTimeMillis();

        BigInteger sigmaTemp = new BigInteger(sigma);
        byte[] temp = sigmaTemp.toByteArray();
        byte[] key = hashMD5(temp);

        String result = "";
        result = decryptAES(encKey, key);
        result = result.replaceAll("\uFFFD", "");
        result = result.replaceAll("\u0000", "");    //???

        long endTime = System.currentTimeMillis();
        Log.i(TIME_LOG_TAG, "decryptStoredKey(): " + (endTime - beginTime));
        return result;
    }

    public BigInteger generateR() {
        SecureRandom rGenerator;
        byte[] rBytes;
        BigInteger r = null;

        try {
            long beginTime = System.currentTimeMillis();
            rGenerator = SecureRandom.getInstance("SHA1PRNG");
            rBytes = new byte[128];

            rGenerator.nextBytes(rBytes);
            r = new BigInteger(1, rBytes);
            long endTime = System.currentTimeMillis();
            Log.i(TIME_LOG_TAG, "generateR(): " + (endTime - beginTime));
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return r;
    }

    public String getRasString() {
        return generateR().toString();
    }

    public String getSubscriptionRand(int id) throws Exception {
        ViewerTag tag = dbHelper.getTagItem(id);
        return tag.getR();
    }

    public static String hashPw(String pass) {
        try {
            byte[] passbytes = pass.getBytes();
            MessageDigest mdigest = MessageDigest.getInstance("MD5");
            mdigest.update(passbytes);
            byte hashBytes[] = mdigest.digest();
            StringBuffer sbuffer = new StringBuffer();
            for (int i = 0; i < hashBytes.length; i++) {
                String temp = Integer.toHexString(0xff & hashBytes[i]);
                if (temp.length() == 1)
                    sbuffer.append('0');
                sbuffer.append(temp);
            }

            pass = sbuffer.toString();
        } catch (Exception e) {
            return pass;
        }

        return pass;
    }
}
