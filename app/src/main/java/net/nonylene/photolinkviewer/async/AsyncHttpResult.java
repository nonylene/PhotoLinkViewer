package net.nonylene.photolinkviewer.async;

public class AsyncHttpResult<Bitmap> {
    private Bitmap bitmap;
    private String url;
    private String type;
    private int height;
    private int width;

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

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public void setSize(int width, int height) {
        this.height = height;
        this.width = width;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
