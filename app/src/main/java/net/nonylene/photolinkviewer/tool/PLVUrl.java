package net.nonylene.photolinkviewer.tool;

import android.os.Parcel;
import android.os.Parcelable;

public class PLVUrl implements Parcelable{
    private String url;
    private String displayUrl;
    private String biggestUrl;
    private String siteName;
    private String fileName;
    private String type;
    private int height;
    private int width;

    public PLVUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getBiggestUrl() {
        return biggestUrl;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setDisplayUrl(String displayUrl) {
        this.displayUrl = displayUrl;
    }

    public void setBiggestUrl(String biggestUrl) {
        this.biggestUrl = biggestUrl;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(displayUrl);
        dest.writeString(biggestUrl);
        dest.writeString(siteName);
        dest.writeString(fileName);
        dest.writeString(type);
    }

    public static final Parcelable.Creator<PLVUrl> CREATOR = new Parcelable.Creator<PLVUrl>(){
        @Override
        public PLVUrl createFromParcel(Parcel source) {
            return new PLVUrl(source);
        }

        @Override
        public PLVUrl[] newArray(int size) {
            return new PLVUrl[size];
        }
    };

    public PLVUrl(Parcel source){
        this.url = source.readString();
        this.displayUrl = source.readString();
        this.biggestUrl = source.readString();
        this.siteName = source.readString();
        this.fileName = source.readString();
        this.type = source.readString();
    }
}
