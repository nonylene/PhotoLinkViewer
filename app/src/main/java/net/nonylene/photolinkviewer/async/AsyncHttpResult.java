package net.nonylene.photolinkviewer.async;

public class AsyncHttpResult<Bitmap> {
    private Bitmap bitmap;
    private String url;

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
