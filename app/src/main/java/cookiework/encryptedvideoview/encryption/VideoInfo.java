package cookiework.encryptedvideoview.encryption;

import android.os.Parcel;
import android.os.Parcelable;

import org.spongycastle.util.encoders.UrlBase64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/01/12.
 */
public class VideoInfo implements Parcelable{
    private int id;
    private String cipherTitle;
    private String cipherIntro;
    private String cipherAddr;
    private String status;
    private String encKey;

    public VideoInfo(){}

    protected VideoInfo(Parcel in) {
        id = in.readInt();
        cipherTitle = in.readString();
        cipherIntro = in.readString();
        cipherAddr = in.readString();
        status = in.readString();
        encKey = in.readString();
    }

    public static final Creator<VideoInfo> CREATOR = new Creator<VideoInfo>() {
        @Override
        public VideoInfo createFromParcel(Parcel in) {
            return new VideoInfo(in);
        }

        @Override
        public VideoInfo[] newArray(int size) {
            return new VideoInfo[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEncKey() {
        return encKey;
    }

    public void setEncKey(String encKey) {
        this.encKey = encKey;
    }

    public String getCipherTitle() {
        return cipherTitle;
    }

    public void setCipherTitle(String cipherTitle) {
        this.cipherTitle = cipherTitle;
    }

    public String getCipherIntro() {
        return cipherIntro;
    }

    public void setCipherIntro(String cipherIntro) {
        this.cipherIntro = cipherIntro;
    }

    public String getCipherAddr() {
        return cipherAddr;
    }

    public void setCipherAddr(String cipherAddr) {
        this.cipherAddr = cipherAddr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return cipherTitle;
    }

    public static void decryptVideoInfo(PtWittEnc enc, ArrayList<VideoInfo> list, SubscriptionInfo subInfo){
        if(list == null) return;
        for(VideoInfo info : list){
            String sigma = enc.dbHelper.getTagItem(subInfo.getId()).getSigma();
            byte[] key = UrlBase64.decode(enc.decryptStoredKey(info.getEncKey(), sigma));
            info.setCipherTitle(enc.decryptAES(info.getCipherTitle(), key));
            info.setCipherIntro(enc.decryptAES(info.getCipherIntro(), key));
            info.setCipherAddr(enc.decryptAES(info.getCipherAddr(), key));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        /*
        private int id;
    private String cipherTitle;
    private String cipherIntro;
    private String cipherAddr;
    private String status;
    private String encKey;
    private HashMap<String, String> tagsAndEncKeys = new HashMap<>();
         */
        dest.writeInt(id);
        dest.writeString(cipherTitle);
        dest.writeString(cipherIntro);
        dest.writeString(cipherAddr);
        dest.writeString(status);
        dest.writeString(encKey);
    }
}
