package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.async.AsyncGetURL;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PLVUrlService {
    private PLVUrlListener plvUrlListener;
    private Context context;

    public PLVUrlService(Context context) {
        this.context = context;
    }

    public interface PLVUrlListener {
        void onGetPLVUrlFinished(PLVUrl plvUrl);
        void onGetPLVUrlFailed(String text);
        void onURLAccepted();
    }

    public void setPLVUrlListener(PLVUrlListener urlListener) {
        plvUrlListener = urlListener;
    }

    public void requestGetPLVUrl(String url) {
        Site site;
        if (url.contains("flickr.com") || url.contains("flic.kr")) {
            site = new FlickrSite(url, context);
        } else if (url.contains("nico.ms") || url.contains("seiga.nicovideo.jp")) {
            site = new NicoSite(url, context);
        } else if (url.contains("pixiv.net")) {
            site = new PixivSite(url, context);
        } else if (url.contains("twimg.com/media/")) {
            site = new TwitterSite(url, context);
        } else if (url.contains("twipple.jp")) {
            site = new TwippleSite(url, context);
        } else if (url.contains("img.ly")) {
            site = new ImglySite(url, context);
        } else if (url.contains("instagram.com") || url.contains("instagr.am")) {
            site = new InstagramSite(url, context);
        } else if (url.contains("gyazo.com")) {
            site = new GyazoSite(url, context);
        } else if (url.contains("imgur.com")) {
            site = new ImgurSite(url, context);
        } else {
            site = new OtherSite(url, context);
        }
        site.setPLVUrlListener(plvUrlListener);
        site.getPLVUrl();
    }

    private class Site {
        protected String url;
        protected PLVUrlListener listener;
        protected Context context;

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

        protected String getQuality(String siteName) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            String quality;
            if (wifiChecker(sharedPreferences)) {
                quality = sharedPreferences.getString(siteName + "_quality_wifi", "large");
            } else {
                quality = sharedPreferences.getString(siteName + "_quality_3g", "large");
            }

            return quality;
        }

        protected void onParseFailed() {
            listener.onGetPLVUrlFailed(context.getString(R.string.url_purse_toast));
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
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("twitter");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = null;
            switch (super.getQuality("twitter")) {
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
            plvUrl.setThumbUrl(url + ":small");
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
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("twipple");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = null;
            switch (super.getQuality("twipple")) {
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
            plvUrl.setThumbUrl("http://p.twipple.jp/show/large/" + id);
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
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("img.ly");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = null;
            switch (super.getQuality("imgly")) {
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
            plvUrl.setThumbUrl("http://img.ly/show/medium/" + id);
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
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("instagram");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = null;
            switch (super.getQuality("instagram")) {
                case "large":
                    file_url = "https://instagram.com/p/" + id + "/media/?size=l";
                    break;
                case "medium":
                    file_url = "https://instagram.com/p/" + id + "/media/?size=m";
                    break;
            }
            plvUrl.setDisplayUrl(file_url);
            plvUrl.setThumbUrl("https://instagram.com/p/" + id + "/media/?size=m");
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
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("gyazo");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = "https://gyazo.com/" + id + "/raw";
            plvUrl.setDisplayUrl(file_url);

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
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("imgur");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            String file_url = "http://i.imgur.com/" + id + ".jpg";
            plvUrl.setDisplayUrl(file_url);

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
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("other");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            plvUrl.setDisplayUrl(url);

            listener.onGetPLVUrlFinished(plvUrl);
        }
    }

    private class PixivSite extends Site {

        public PixivSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            final PLVUrl plvUrl = new PLVUrl(url);
            Pattern pattern = Pattern.compile("^https?://.*pixiv\\.net/member_illust.php?.*illust_id=(\\d+)");
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("pixiv");

            String id = matcher.group(1);
            plvUrl.setFileName(id);

            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(new PXVStringRequest(context, id,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            final String[] list = response.split(",", -1);
                            // get original photo
                            try {
                                String file_url = list[9].replaceAll("\"", "");

                                plvUrl.setDisplayUrl(file_url);

                                listener.onGetPLVUrlFinished(plvUrl);

                            } catch (ArrayIndexOutOfBoundsException e) {
                                listener.onGetPLVUrlFailed("Cannot open. R-18?");
                            }
                        }
                    }));
        }
    }

    private class FlickrSite extends Site {

        public FlickrSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            final PLVUrl plvUrl = new PLVUrl(url);

            String id = null;
            if (url.contains("flickr")) {
                Pattern pattern = Pattern.compile("^https?://[wm]w*\\.flickr\\.com/?#?/photos/[\\w@]+/(\\d+)");
                Matcher matcher = pattern.matcher(url);
                if (!matcher.find()) {
                    super.onParseFailed();
                    return;
                }
                id = matcher.group(1);
            } else if (url.contains("flic.kr")) {
                Pattern pattern = Pattern.compile("^https?://flic\\.kr/p/(\\w+)");
                Matcher matcher = pattern.matcher(url);
                if (!matcher.find()) {
                    super.onParseFailed();
                    return;
                }
                id = Base58.decode(matcher.group(1));
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("flickr");

            plvUrl.setFileName(id);

            String api_key = (String) context.getText(R.string.flickr_key);
            String request = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&format=json&api_key=" + api_key +
                    "&photo_id=" + id;

            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(new MyJsonObjectRequest(context, request,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                listener.onGetPLVUrlFinished(parseFlickr(response, plvUrl));
                            } catch (JSONException e) {
                                listener.onGetPLVUrlFailed(context.getString(R.string.show_flickrjson_toast));
                                e.printStackTrace();
                            }
                        }
                    }
            ));
        }

        private PLVUrl parseFlickr(JSONObject json, PLVUrl plvUrl) throws JSONException {
            //for flickr
            JSONObject photo = new JSONObject(json.getString("photo"));
            String farm = photo.getString("farm");
            String server = photo.getString("server");
            String id = photo.getString("id");
            String secret = photo.getString("secret");

            String file_url = null;
            switch (super.getQuality("flickr")) {
                case "original":
                    String original_secret = photo.getString("originalsecret");
                    String original_format = photo.getString("originalformat");
                    file_url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + original_secret + "_o." + original_format;
                    break;
                case "large":
                    file_url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_b.jpg";
                    break;
                case "medium":
                    file_url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_z.jpg";
                    break;
            }


            String original_secrets = photo.getString("originalsecret");
            String original_formats = photo.getString("originalformat");
            String biggestUrl = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + original_secrets + "_o." + original_formats;

            plvUrl.setDisplayUrl(file_url);
            plvUrl.setThumbUrl("https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_z.jpg");
            plvUrl.setBiggestUrl(biggestUrl);

            return plvUrl;
        }
    }

    private class NicoSite extends Site {

        public NicoSite(String url, Context context) {
            super(url, context);
        }

        @Override
        public void getPLVUrl() {
            super.getPLVUrl();

            final PLVUrl plvUrl = new PLVUrl(url);

            Pattern pattern;
            if (url.contains("nico.ms")) {
                pattern = Pattern.compile("^https?://nico\\.ms/im(\\d+)");
            } else {
                pattern = Pattern.compile("^https?://seiga.nicovideo.jp/seiga/im(\\d+)");
            }
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) {
                super.onParseFailed();
                return;
            }

            listener.onURLAccepted();

            plvUrl.setSiteName("nico");

            final String id = matcher.group(1);
            plvUrl.setFileName(id);

            String oldUrl = "http://seiga.nicovideo.jp/image/source/" + id;

            AsyncTask<String, Integer, String> task = new AsyncGetURL() {
                @Override
                protected void onPostExecute(String redirect) {
                    super.onPostExecute(redirect);
                    listener.onGetPLVUrlFinished(parseNico(redirect, id, plvUrl));
                }
            };

            task.execute(oldUrl);
        }

        private PLVUrl parseNico(String redirect, String id, PLVUrl plvUrl) {
            String biggest_url = redirect.replace("/o/", "/priv/");

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean original;
            if (wifiChecker(sharedPreferences)) {
                original = sharedPreferences.getBoolean("original_switch_wifi", false);
            } else {
                original = sharedPreferences.getBoolean("original_switch_3g", false);
            }

            String quality = super.getQuality("nicoseiga");

            String file_url = null;
            switch (quality) {
                case "original":
                    file_url = biggest_url;
                    break;
                case "large":
                    file_url = "http://lohas.nicoseiga.jp/img/" + id + "l";
                    break;
                case "medium":
                    file_url = "http://lohas.nicoseiga.jp/img/" + id + "m";
                    break;
            }

            if (redirect.contains("account.nicovideo.jp") && (original || quality.equals("original"))) {
                // cannot preview original photo
                biggest_url = "http://lohas.nicoseiga.jp/img/" + id + "l";
                Toast.makeText(context, context.getString(R.string.nico_original_toast), Toast.LENGTH_LONG).show();
            }

            plvUrl.setDisplayUrl(file_url);
            plvUrl.setThumbUrl("http://lohas.nicoseiga.jp/img/" + id + "m");
            plvUrl.setBiggestUrl(biggest_url);

            return plvUrl;
        }
    }
}
