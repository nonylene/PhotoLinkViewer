package net.nonylene.photolinkviewer.async;

import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public class AsyncGetSizeType extends AsyncTask<String, Integer, AsyncGetSizeType.Result>{

    @Override
    protected Result doInBackground(String... params) {
        Result result = null;

        try {
            // read binary and get type
            byte[] yaBinary = new byte[4];

            URL url = new URL(params[0]);
            InputStream inputStream = url.openStream();
            inputStream.read(yaBinary, 0, 4);
            inputStream.close();

            // get bitmap size
            inputStream = url.openStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            result = new Result();
            result.setType(getFileType(yaBinary));
            result.setHeight(options.outHeight);
            result.setWidth(options.outWidth);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private String getFileType(byte[] head) {
        // get file type from binary
        byte[] png = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};
        byte[] gif = {(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38};
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        byte[] bmp = {(byte) 0x42, (byte) 0x4D};

        if (Arrays.equals(Arrays.copyOfRange(head, 0, 4), png)) return "png";
        else if (Arrays.equals(Arrays.copyOfRange(head, 0, 4), gif)) return "gif";
        else if (Arrays.equals(Arrays.copyOfRange(head, 0, 3), jpg)) return "jpg";
        else if (Arrays.equals(Arrays.copyOfRange(head, 0, 2), bmp)) return "bmp";
        else return "unknown";
    }

    public class Result{
        private int height;
        private int width;
        private String type;

        public void setHeight(int height) {
            this.height = height;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public String getType() {
            return type;
        }
    }
}
