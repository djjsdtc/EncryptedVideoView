/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cookiework.encryptedvideoview.encryption;
import android.content.Context;
import android.util.Log;

import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.UrlBase64;

import java.math.BigInteger;
import java.security.MessageDigest;

import cookiework.encryptedvideoview.util.DBHelper;
import cookiework.encryptedvideoview.util.ViewerTag;

import static cookiework.encryptedvideoview.Constants.ECCURVE;
import static cookiework.encryptedvideoview.Constants.ECCURVE_NAME;
import static cookiework.encryptedvideoview.Constants.TIME_LOG_TAG;

/**
 *
 * @author Andrew
 *
 * This class is designed encapsulate methods that are used in the subscription process.
 * Subscribe, Approve and Finalize.  It uses the methods from PtwittEnc to accomplish
 * this task and provide the Firefox extension an simplier way to do different subscription methods.
 */
public class SubscriptionProcessor {

    private PtWittEnc enc;
    private DBHelper dbHelper;
    
    //Constructor generates random 'r' value from a using the java secruity secure random class with specified number of bits
    public SubscriptionProcessor(Context context)
    {
        this.enc = new PtWittEnc(context);
        this.dbHelper = new DBHelper(context);
    }

    public PtWittEnc getEnc() {
        return enc;
    }

    public ECPoint generateRequest(String tag, BigInteger r)
    {
        // M = f(H(tag))
        // m = rM
        long beginTime = System.currentTimeMillis();
        ECPoint M = null;

        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            digest.update(tag.getBytes());
            byte[] result = digest.digest();

            final BigInteger p = ECCURVE.getQ();
            BigInteger xVal = new BigInteger(1, result).mod(p);
            ECFieldElement y;
            while(true) {
                ECFieldElement x = ECCURVE.fromBigInteger(xVal);
                ECFieldElement a = ECCURVE.getA();
                ECFieldElement b = ECCURVE.getB();
                // y = (x^3 + ax + b)^0.5
                ECFieldElement rhs = x.square().add(a).multiply(x).add(b);
                y = rhs.sqrt();
                System.out.println(y);
                if(y != null){
                    break;
                } else {
                    xVal = xVal.add(BigInteger.ONE).mod(p);
                }
            }
            ECPoint hashPt = ECCURVE.createPoint(xVal, y.toBigInteger());

            M = hashPt.multiply(r);

            long endTime = System.currentTimeMillis();
            Log.i(TIME_LOG_TAG, "generateRequest(): " + (endTime - beginTime));
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return M;
    }

    public String getRequestString(String tag, String rStr)
    {
        BigInteger r = new BigInteger(rStr);

        ECPoint M = generateRequest(tag, r);
        String mStr = new String(UrlBase64.encode(M.getEncoded(true)));
        return mStr;
    }

    public String processResponse(ECPoint mPrime, BigInteger r, int id)
    {
        // sigma = mPrime / r
        // t = H2(sigma)
        long beginTime = System.currentTimeMillis();

        X9ECParameters curveParameters = ECUtil.getNamedCurveByName(ECCURVE_NAME);
        BigInteger N = curveParameters.getN();
        BigInteger rReverse = r.modInverse(N);
        ECPoint sigmaPoint = mPrime.multiply(rReverse);
        BigInteger sigma = new BigInteger(sigmaPoint.getEncoded(true));
        //BigInteger sigma = sigmaPoint.normalize().getXCoord().toBigInteger();
        sigmaToFile(sigma, id);
        byte[] temp = sigma.toByteArray();
        String t = enc.createTStar(temp);

        long endTime = System.currentTimeMillis();
        Log.i(TIME_LOG_TAG, "processResponse(): " + (endTime - beginTime));

        return t;
    }

    public String processResponseString(String mPrimeStr, String RStr, int id)
    {
        long beginTime = System.currentTimeMillis();

        byte[] mPrimeByte = UrlBase64.decode(mPrimeStr);
        ECPoint mPrime = ECCURVE.decodePoint(mPrimeByte);
        BigInteger R = new BigInteger(RStr);

        long endTime = System.currentTimeMillis();
        Log.i(TIME_LOG_TAG, "decodePoint(): " + (endTime - beginTime));

        return processResponse(mPrime, R, id);
    }

    public void sigmaToFile(BigInteger sigma, int id)
    {
        ViewerTag tag = dbHelper.getTagItem(id);
        tag.setSigma(sigma.toString());
        dbHelper.updateTagItem(tag);
    }

    public ECPoint tagReplace(ECPoint sigmaPoint, BigInteger r)
    {
        // m = r*sigma
        long beginTime = System.currentTimeMillis();
        ECPoint M = null;

        try
        {
            M = sigmaPoint.multiply(r);

            long endTime = System.currentTimeMillis();
            Log.i(TIME_LOG_TAG, "tagReplace(): " + (endTime - beginTime));
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return M;
    }

    public String tagReplaceString(int id, String rStr)
    {
        BigInteger r = new BigInteger(rStr);
        BigInteger sigmaPointByte = new BigInteger(dbHelper.getTagItem(id).getSigma());
        ECPoint sigmaPoint = ECCURVE.decodePoint(sigmaPointByte.toByteArray());

        ECPoint M = tagReplace(sigmaPoint, r);
        String mStr = new String(UrlBase64.encode(M.getEncoded(true)));
        return mStr;
    }
}
