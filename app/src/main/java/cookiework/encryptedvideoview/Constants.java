package cookiework.encryptedvideoview;

import org.spongycastle.math.ec.custom.sec.SecP256K1Curve;

/**
 * Created by Cookie on 2017-01-15.
 */

public interface Constants {
    String SERVER_ADDRESS = "http://10.21.238.153:8080/encryptvideoweb";
    String PLAY_ADDRESS = "http://10.21.238.153/hls/[stream_name].m3u8";
    String PLAY_VOD_ADDRESS = "http://10.21.238.153/videos/[stream_name].m3u8";
    //replace [stream_name] with real stream name
    String ECCURVE_NAME = "secp256k1";
    SecP256K1Curve ECCURVE = new SecP256K1Curve();
    String SHARED_PREFERENCES = "cookiework.encryptedvideoview.sp";
    int CONNECT_TIMEOUT = 8000;
    int READ_TIMEOUT = 8000;
    String DB_NAME = "viewer_db";
    int DB_VERSION = 1;
    String TIME_LOG_TAG = "TIME_LOG";
}
