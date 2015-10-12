package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.toolbox.ImageLoader;

import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.view.UserTweetView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Status;

public class TwitterStatusAdapter extends BaseAdapter implements UserTweetView.TwitterViewListener {

    private List<Status> statusList = new ArrayList<>();
    private TwitterAdapterListener twitterAdapterListener;
    private ImageLoader imageLoader;
    private Animation rotateAnimation;
    private boolean isRequesting;
    private int selectableBackgroundId;

    public TwitterStatusAdapter(Context context, ImageLoader imageLoader) {
        rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate);
        this.imageLoader = imageLoader;
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        selectableBackgroundId = outValue.resourceId;
    }

    @Override
    public void onShowFragmentRequired(@NotNull PLVUrl plvUrl) {
        twitterAdapterListener.onShowFragmentRequired(plvUrl);
    }

    @Override
    public void onVideoShowFragmentRequired(@NotNull PLVUrl plvUrl) {
        twitterAdapterListener.onVideoShowFragmentRequired(plvUrl);
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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (getItemViewType(position) == ItemType.STATUS.getId()) {
            if (convertView == null) convertView = inflater.inflate(R.layout.twitter_status_list, parent, false);

            UserTweetView userTweetView = (UserTweetView) convertView;
            Status status = getItem(position);
            // wrap_content -> called getView multiple -> check if status is the same
            if (status != userTweetView.getTag(R.id.STATUS_TAG)) {
                userTweetView.setTag(R.id.STATUS_TAG, status);
                userTweetView.setImageLoader(imageLoader);
                userTweetView.setTwitterViewListener(this);
                userTweetView.setEntry(getItem(position));
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

    public interface TwitterAdapterListener {
        void onShowFragmentRequired(PLVUrl plvUrl);

        void onVideoShowFragmentRequired(PLVUrl plvUrl);

        void onReadMoreClicked();
    }
}
