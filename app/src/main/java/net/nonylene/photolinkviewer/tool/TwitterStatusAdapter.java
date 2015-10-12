package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.volley.toolbox.ImageLoader;

import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.view.UserTweetLoadingView;
import net.nonylene.photolinkviewer.view.UserTweetView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Status;

public class TwitterStatusAdapter extends BaseAdapter implements UserTweetView.TwitterViewListener , UserTweetLoadingView.LoadingViewListener{

    private List<Status> statusList = new ArrayList<>();
    private TwitterAdapterListener twitterAdapterListener;
    private ImageLoader imageLoader;
    private boolean isRequesting;

    public TwitterStatusAdapter(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    @Override
    public void onShowFragmentRequired(@NotNull PLVUrl plvUrl) {
        twitterAdapterListener.onShowFragmentRequired(plvUrl);
    }

    @Override
    public void onVideoShowFragmentRequired(@NotNull PLVUrl plvUrl) {
        twitterAdapterListener.onVideoShowFragmentRequired(plvUrl);
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
            if (convertView == null) convertView = inflater.inflate(R.layout.loading_layout, parent, false);

            UserTweetLoadingView userTweetLoadingView = (UserTweetLoadingView) convertView;
            userTweetLoadingView.setLoadingViewListener(this);
            userTweetLoadingView.setIsRequesting(isRequesting);
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

    @Override
    public void onReadMoreClicked() {
        twitterAdapterListener.onReadMoreClicked();
        setRequesting(true);
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
