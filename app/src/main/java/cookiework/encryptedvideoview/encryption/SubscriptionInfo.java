package cookiework.encryptedvideoview.encryption;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Created by Administrator on 2017/01/12.
 */
public class SubscriptionInfo implements Parcelable{
    public static final String SUBSCRIPTION_APPROVE = "approve";
    public static final String SUBSCRIPTION_REPLACE = "replace";

    private int id;
    private String userID;
    private String status;
    private String m;
    private String destUserID;
    private String tagName;
    private String MPrime;

    protected SubscriptionInfo(Parcel in) {
        id = in.readInt();
        userID = in.readString();
        status = in.readString();
        m = in.readString();
        destUserID = in.readString();
        tagName = in.readString();
        MPrime = in.readString();
    }

    public static final Creator<SubscriptionInfo> CREATOR = new Creator<SubscriptionInfo>() {
        @Override
        public SubscriptionInfo createFromParcel(Parcel in) {
            return new SubscriptionInfo(in);
        }

        @Override
        public SubscriptionInfo[] newArray(int size) {
            return new SubscriptionInfo[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getM() {
        return m;
    }

    public void setM(String m) {
        this.m = m;
    }

    public String getDestUserID() {
        return destUserID;
    }

    public void setDestUserID(String destUserID) {
        this.destUserID = destUserID;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getMPrime() {
        return MPrime;
    }

    public void setMPrime(String MPrime) {
        this.MPrime = MPrime;
    }

    @Override
    public String toString() {
        return destUserID + " - " + status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(userID);
        dest.writeString(status);
        dest.writeString(m);
        dest.writeString(destUserID);
        dest.writeString(tagName);
        dest.writeString(MPrime);
    }
}
