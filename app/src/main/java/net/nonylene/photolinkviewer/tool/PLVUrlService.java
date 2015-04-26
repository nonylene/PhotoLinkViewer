package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PLVUrlService {
    private PLVUrlListener plvUrlListener;
    private Context context;

    public PLVUrlService(Context context) {
        this.context = context;
    }

    public interface PLVUrlListener {
        public void onGetPLVUrlFinished(PLVUrl plvUrl);
    }

    public void setPLVUrlListener(PLVUrlListener urlListener) {
        plvUrlListener = urlListener;
    }

    public void requestGetPLVUrl(String url) {
        Site site = new Site(url, context);
        site.setPLVUrlListener(plvUrlListener);
        // if~~~
    }

    private class Site {
        protected String url;
        private Context context;
        protected PLVUrlListener listener;

        public Site(String url, Context context) {
            this.url = url;
            this.context = context;
        }

        public void setPLVUrlListener(PLVUrlListener listener) {
            this.listener = listener;
        }

        protected boolean wifiChecker(SharedPreferences sharedPreferences) {
            //check wifi connecting and setting or not
            boolean wifi = false;
            if (sharedPreferences.getBoolean("wifi_switch", false)) {
                // get wifi status
                ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = manager.getActiveNetworkInfo();
                if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                    wifi = true;
                }
            }
            return wifi;
        }

        public void getPLVUrl() {
        }
    }

    private class TwitterSite extends Site {

        public TwitterSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("^https?://pbs\\.twimg\\.com/media/([^\\.]+)\\.");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) return;

            plvUrl.setSiteName("twitter");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            String quality;
            if (super.wifiChecker(sharedPreferences)) {
                quality = sharedPreferences.getString("twitter_quality_wifi", "large");
            } else {
                quality = sharedPreferences.getString("twitter_quality_3g", "large");
            }

            String file_url = null;
            switch (quality) {
                case "original":
                    file_url = url + ":orig";
                    break;
                case "large":
                    file_url = url + ":large";
                    break;
                case "medium":
                    file_url = url;
                    break;
                case "small":
                    file_url = url + ":small";
                    break;
            }
            plvUrl.setDisplayUrl(file_url);

            plvUrl.setBiggestUrl(url + ":orig");

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }

    private class TwippleSite extends Site {

        public TwippleSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("^https?://p\\.twipple\\.jp/(\\w+)");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) return;

            plvUrl.setSiteName("twipple");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            String quality;
            if (super.wifiChecker(sharedPreferences)) {
                quality = sharedPreferences.getString("twipple_quality_wifi", "large");
            } else {
                quality = sharedPreferences.getString("twipple_quality_3g", "large");
            }

            String file_url = null;
            switch (quality) {
                case "original":
                    file_url = "http://p.twipple.jp/show/orig/" + id;
                    break;
                case "large":
                    file_url = "http://p.twipple.jp/show/large/" + id;
                    break;
                case "thumb":
                    file_url = "http://p.twipple.jp/show/thumb/" + id;
                    break;
            }
            plvUrl.setDisplayUrl(file_url);

            plvUrl.setBiggestUrl("http://p.twipple.jp/show/orig/" + id);

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }

    private class ImglySite extends Site {

        public ImglySite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("^https?://img\\.ly/(\\w+)");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) return;

            plvUrl.setSiteName("img.ly");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            String quality;
            if (super.wifiChecker(sharedPreferences)) {
                quality = sharedPreferences.getString("imgly_quality_wifi", "large");
            } else {
                quality = sharedPreferences.getString("imgly_quality_3g", "large");
            }

            String file_url = null;
            switch (quality) {
                case "full":
                    file_url = "http://img.ly/show/full/" + id;
                    break;
                case "large":
                    file_url = "http://img.ly/show/large/" + id;
                    break;
                case "medium":
                    file_url = "http://img.ly/show/medium/" + id;
                    break;
            }
            plvUrl.setDisplayUrl(file_url);

            plvUrl.setBiggestUrl("http://img.ly/show/full/" + id);

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }

    private class InstagramSite extends Site {

        public InstagramSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("^https?://instagr\\.?am[\\.com]*/p/([^/\\?=]+)");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) return;

            plvUrl.setSiteName("instagram");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            String quality;
            if (super.wifiChecker(sharedPreferences)) {
                quality = sharedPreferences.getString("instagram_quality_wifi", "large");
            } else {
                quality = sharedPreferences.getString("instagram_quality_3g", "large");
            }

            String file_url = null;
            switch (quality) {
                case "large":
                    file_url = "https://instagram.com/p/" + id + "/media/?size=l";
                    break;
                case "medium":
                    file_url = "https://instagram.com/p/" + id + "/media/?size=m";
                    break;
            }
            plvUrl.setDisplayUrl(file_url);

            plvUrl.setBiggestUrl("https://instagram.com/p/" + id + "/media/?size=l");

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }

    private class GyazoSite extends Site {

        public GyazoSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("^https?://.*gyazo\\.com/(\\w+)");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) return;

            plvUrl.setSiteName("gyazo");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = "https://gyazo.com/" + id + "/raw";
            plvUrl.setDisplayUrl(file_url);
            plvUrl.setBiggestUrl(file_url);

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }

    private class ImgurSite extends Site {

        public ImgurSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("^https?://.*imgur\\.com/([\\w^\\.]+)");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) return;

            plvUrl.setSiteName("imgur");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = "http://i.imgur.com/" + id + ".jpg";
            plvUrl.setDisplayUrl(file_url);
            plvUrl.setBiggestUrl(file_url);

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }

    private class OtherSite extends Site {

        public OtherSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("/([^\\./]+)\\.?[\\w\\?=]*$");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) return;

            plvUrl.setSiteName("other");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            plvUrl.setDisplayUrl(url);
            plvUrl.setBiggestUrl(url);

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }
}
