package net.nonylene.photolinkviewer.tool;

public class PLVUrl {
    private String url;
    private String displayUrl;
    private String biggestUrl;
    private String siteName;
    private String fileName;

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
}
