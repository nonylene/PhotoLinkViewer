package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import net.nonylene.photolinkviewer.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;

public class TwitterStatusAdapter extends BaseAdapter {

    private List<Status> statusList = new ArrayList<>();
    private TwitterAdapterListener twitterAdapterListener;
    private Context baseContext;
    private ImageLoader imageLoader;
    private Animation rotateAnimation;
    private boolean isRequesting;
    private int selectableBackgroundId;

    public TwitterStatusAdapter(Context context, ImageLoader imageLoader) {
        baseContext = context;
        rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate);
        this.imageLoader = imageLoader;
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        selectableBackgroundId = outValue.resourceId;
    }

    private class StatusViewHolder {
        TextView textView;
        TextView snView;
        TextView dayView;
        TextView favView;
        TextView rtView;
        NetworkImageView iconView;
        LinearLayout urlBaseLayout;
        LinearLayout urlLayout;
        LinearLayout urlPhotoLayout;
        LinearLayout photoBaseLayout;
        LinearLayout photoLayout;

        public void setView(View baseView) {
            textView = (TextView) baseView.findViewById(R.id.twTxt);
            snView = (TextView) baseView.findViewById(R.id.twSN);
            dayView = (TextView) baseView.findViewById(R.id.twDay);
            favView = (TextView) baseView.findViewById(R.id.favCount);
            rtView = (TextView) baseView.findViewById(R.id.rtCount);
            iconView = (NetworkImageView) baseView.findViewById(R.id.twImageView);
            urlLayout = (LinearLayout) baseView.findViewById(R.id.url_linear);
            urlBaseLayout = (LinearLayout) baseView.findViewById(R.id.url_base);
            urlPhotoLayout = (LinearLayout) baseView.findViewById(R.id.url_photos);
            photoBaseLayout = (LinearLayout) baseView.findViewById(R.id.photo_base);
            photoLayout = (LinearLayout) baseView.findViewById(R.id.photos);
        }
    }

    private class LoadingViewHolder {
        LinearLayout baseView;
        ImageView loadingView;

        public void setView(View baseView) {
            this.baseView = (LinearLayout) baseView;
            loadingView = (ImageView) baseView.findViewById(R.id.loading_imageview);
        }

    }

    @Override
    public int getCount() {
        return statusList.size();
    }

    @Override
    public Status getItem(int position) {
        return statusList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) == null ? ItemType.LOADING.getId() : ItemType.STATUS.getId();
    }

    @Override
    public int getViewTypeCount() {
        return ItemType.values().length;
    }

    // not selectable base view
    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != ItemType.STATUS.getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StatusViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (getItemViewType(position) == ItemType.STATUS.getId()) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.twitter_status_list, parent, false);
                viewHolder = new StatusViewHolder();
                viewHolder.setView(convertView);
                convertView.setTag(R.id.VIEW_HOLDER_TAG, viewHolder);

            } else {
                viewHolder = (StatusViewHolder) convertView.getTag(R.id.VIEW_HOLDER_TAG);
            }

            Status status = getItem(position);
            // wrap_content -> called getView multiple -> check if status is the same
            if (status != convertView.getTag(R.id.STATUS_TAG)) {
                convertView.setTag(R.id.STATUS_TAG, status);
                setEntry(getItem(position), viewHolder, parent);
            }

        } else if (getItemViewType(position) == ItemType.LOADING.getId()) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.loading_layout, parent, false);
                LoadingViewHolder loadingViewHolder = new LoadingViewHolder();
                loadingViewHolder.setView(convertView);
                convertView.setTag(loadingViewHolder);
            }

            final LoadingViewHolder loadingViewHolder = (LoadingViewHolder) convertView.getTag();

            if (isRequesting) {
                // loading now...
                convertView.setBackgroundResource(android.R.color.transparent);
                loadingViewHolder.loadingView.setAnimation(rotateAnimation);

                convertView.setOnClickListener(null);

            } else {
                // not loading now...
                convertView.setBackgroundResource(selectableBackgroundId);
                loadingViewHolder.loadingView.setAnimation(null);

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isRequesting) {
                            isRequesting = true;
                            // unset loading callback and background
                            loadingViewHolder.baseView.setBackgroundResource(android.R.color.transparent);
                            loadingViewHolder.loadingView.setAnimation(rotateAnimation);
                            if (twitterAdapterListener != null)
                                twitterAdapterListener.onReadMoreClicked();
                        }
                    }
                });
            }
        }

        return convertView;
    }

    public void setTwitterAdapterListener(TwitterAdapterListener twitterAdapterListener) {
        this.twitterAdapterListener = twitterAdapterListener;
    }

    public void addItem(Status status) {
        // init -> add loading footer
        if (statusList.isEmpty()) {
            statusList.add(status);
            statusList.add(null);
        } else {
            statusList.add(statusList.size() - 1, status);
        }
    }

    public void setRequesting(boolean isRequesting) {
        this.isRequesting = isRequesting;
    }

    public Status getLastStatus() {
        int size = statusList.size();
        if (statusList.get(size - 1) != null) {
            return statusList.get(size - 1);
        } else {
            return statusList.get(size - 2);
        }
    }

    public void removeLoadingItem() {
        statusList.remove(statusList.size() - 1);
    }

    private enum ItemType {
        STATUS(0),
        LOADING(1);

        private int id;

        ItemType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    private void setEntry(Status status, StatusViewHolder viewHolder, ViewGroup viewGroup) {
        final Context context = viewGroup.getContext();
        // set media entity
        ExtendedMediaEntity[] mediaEntities = status.getExtendedMediaEntities();
        URLEntity[] urlEntities = status.getURLEntities();

        // put status on text
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        //retweet check
        final Status finStatus;

        if (status.isRetweet()) {
            finStatus = status.getRetweetedStatus();
        } else {
            finStatus = status;
        }

        viewHolder.textView.setText(finStatus.getText());

        final String screen = finStatus.getUser().getScreenName();
        viewHolder.snView.setText(finStatus.getUser().getName() + " @" + screen);

        if (finStatus.getUser().isProtected()) {
            // add key icon
            float dp = context.getResources().getDisplayMetrics().density;
            // set size
            int iconSize = (int) (17 * dp);
            // resize app icon (bitmap_factory makes low-quality images)
            Drawable protect = context.getResources().getDrawable(R.drawable.lock);
            protect.setBounds(0, 0, iconSize, iconSize);
            // set app-icon and bounds
            viewHolder.snView.setCompoundDrawables(protect, null, null, null);
        } else {
            // initialize
            viewHolder.snView.setCompoundDrawables(null, null, null, null);
        }

        String statusDate = dateFormat.format(finStatus.getCreatedAt());
        viewHolder.dayView.setText(statusDate);

        // fav and rt
        viewHolder.favView.setText("fav: " + String.valueOf(finStatus.getFavoriteCount()));
        viewHolder.rtView.setText("RT: " + String.valueOf(finStatus.getRetweetCount()));

        // set icon
        NetworkImageView iconView = viewHolder.iconView;
        iconView.setImageUrl(finStatus.getUser().getBiggerProfileImageURL(), imageLoader);

        // set background
        iconView.setBackgroundResource(R.drawable.twitter_image_design);

        //show user when tapped
        iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + screen));
                baseContext.startActivity(intent);
            }
        });

        // initialize
        viewHolder.urlPhotoLayout.removeAllViews();
        viewHolder.urlLayout.removeAllViews();

        if (urlEntities.length > 0) {

            viewHolder.urlBaseLayout.setVisibility(View.VISIBLE);

            final PhotoViewController controller = new PhotoViewController(viewHolder.urlPhotoLayout);

            for (URLEntity urlEntity : urlEntities) {
                String url = urlEntity.getExpandedURL();
                addUrl(url, viewHolder);

                PLVUrlService service = new PLVUrlService(baseContext);
                service.setPLVUrlListener(getPLVUrlListener(controller));

                service.requestGetPLVUrl(url);
            }
        } else {
            viewHolder.urlBaseLayout.setVisibility(View.GONE);
        }

        // initialize
        viewHolder.photoLayout.removeAllViews();
        if (mediaEntities.length > 0) {

            viewHolder.photoBaseLayout.setVisibility(View.VISIBLE);

            final PhotoViewController controller = new PhotoViewController(viewHolder.photoLayout);

            for (ExtendedMediaEntity mediaEntity : mediaEntities) {

                String url = mediaEntity.getMediaURLHttps();

                if (("animated_gif").equals(mediaEntity.getType()) || ("video").equals(mediaEntity.getType())) {
                    String file_url = getBiggestMp4Url(mediaEntity.getVideoVariants());
                    controller.setVideoUrl(controller.addImageView(), mediaEntity.getMediaURLHttps(), file_url);
                } else {

                    PLVUrlService service = new PLVUrlService(baseContext);
                    service.setPLVUrlListener(getPLVUrlListener(controller));

                    service.requestGetPLVUrl(url);
                }
            }
        } else {
            viewHolder.photoBaseLayout.setVisibility(View.GONE);
        }

    }

    private PLVUrlService.PLVUrlListener getPLVUrlListener(final PhotoViewController controller) {
        return new PLVUrlService.PLVUrlListener() {
            int position;

            @Override
            public void onGetPLVUrlFinished(PLVUrl plvUrl) {
                controller.setImageUrl(position, plvUrl);
            }

            @Override
            public void onGetPLVUrlFailed(String text) {

            }

            @Override
            public void onURLAccepted() {
                position = controller.addImageView();
            }
        };
    }

    private void addUrl(final String url, StatusViewHolder viewHolder) {
        // prev is last linear_layout
        LayoutInflater inflater = LayoutInflater.from(viewHolder.urlLayout.getContext());
        TextView textView = (TextView) inflater.inflate(R.layout.twitter_url, viewHolder.urlLayout, false);
        textView.setText(url);
        TextPaint textPaint = textView.getPaint();
        textPaint.setUnderlineText(true);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                baseContext.startActivity(intent);
            }
        });
        viewHolder.urlLayout.addView(textView);
    }

    private String getBiggestMp4Url(ExtendedMediaEntity.Variant[] variants) {
        int bitrate = 0;
        String url = null;
        for (ExtendedMediaEntity.Variant variant : variants) {
            if (("video/mp4").equals(variant.getContentType()) && bitrate <= variant.getBitrate()) {
                url = variant.getUrl();
                bitrate = variant.getBitrate();
            }
        }
        return url;
    }

    private class PhotoViewController {
        private List<NetworkImageView> imageViewList = new ArrayList<>();
        private LinearLayout baseLayout;
        private LayoutInflater inflater;

        public PhotoViewController(LinearLayout baseLayout) {
            this.baseLayout = baseLayout;
            inflater = LayoutInflater.from(baseLayout.getContext());
        }

        public int addImageView() {
            // prev is last linear_layout
            int size = imageViewList.size();

            FrameLayout frameLayout;

            if (size % 2 == 0) {
                // make new linear_layout and put below prev
                LinearLayout new_layout = (LinearLayout) inflater.inflate(R.layout.twitter_photos, baseLayout, false);
                baseLayout.addView(new_layout);
                frameLayout = (FrameLayout) new_layout.getChildAt(0);
            } else {
                LinearLayout prevLayout = (LinearLayout) baseLayout.getChildAt(baseLayout.getChildCount() - 1);
                // put new photo below prev photo (not new linear_layout)
                frameLayout = (FrameLayout) prevLayout.getChildAt(1);
            }
            imageViewList.add((NetworkImageView) frameLayout.getChildAt(0));

            return size;
        }

        public void setImageUrl(int position, final PLVUrl plvUrl) {
            NetworkImageView imageView = imageViewList.get(position);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // go to show fragment
                    if (twitterAdapterListener != null)
                        twitterAdapterListener.onShowFragmentRequired(plvUrl);
                }
            });

            imageView.setImageUrl(plvUrl.getThumbUrl(), imageLoader);
        }

        public void setVideoUrl(int position, String thumbUrl, final String fileUrl) {
            NetworkImageView imageView = imageViewList.get(position);
            FrameLayout frameLayout = (FrameLayout) imageView.getParent();
            ImageView video_icon = (ImageView) frameLayout.getChildAt(1);
            video_icon.setVisibility(View.VISIBLE);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // go to video show fragment
                    if (twitterAdapterListener != null)
                        twitterAdapterListener.onVideoShowFragmentRequired(fileUrl);
                }
            });

            imageView.setImageUrl(thumbUrl, imageLoader);
        }
    }

    public interface TwitterAdapterListener {
        void onShowFragmentRequired(PLVUrl plvUrl);

        void onVideoShowFragmentRequired(String fileUrl);

        void onReadMoreClicked();
    }
}
