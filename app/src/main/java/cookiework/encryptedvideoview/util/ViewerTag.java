package cookiework.encryptedvideoview.util;

/**
 * Created by Administrator on 2017/01/18.
 */

public class ViewerTag {
    //create table viewertag (id int primary key, tag text, r text, sigma text, tStar text)
    private int id;
    private String tag;
    private String r;
    private String sigma;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getR() {
        return r;
    }

    public void setR(String r) {
        this.r = r;
    }

    public String getSigma() {
        return sigma;
    }

    public void setSigma(String sigma) {
        this.sigma = sigma;
    }

}
