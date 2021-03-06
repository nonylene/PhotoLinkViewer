package net.nonylene.photolinkviewer.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import net.nonylene.photolinkviewer.R

class UserTweetLoadingView(context: Context, attr: AttributeSet?) : RelativeLayout(context, attr){

    private val rotateAnimation : Animation = AnimationUtils.loadAnimation(context, R.anim.rotate)
    private val selectableBackgroundId : Int

    private var loadingView : ImageView? = null
    private var loadingBaseView : View? = null

    public var loadingViewListener: LoadingViewListener? = null

    init {
        val outValue = TypedValue();
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        selectableBackgroundId = outValue.resourceId;
        setOnClickListener {
            setIsRequesting(true)
            // unset loading callback and background
            loadingViewListener?.onReadMoreClicked();
        }
    }

    protected override fun onFinishInflate() {
        super.onFinishInflate()
        loadingView = findViewById(R.id.loading_imageview) as ImageView
        loadingBaseView = findViewById(R.id.loading_base_view)
    }

    public fun setIsRequesting(isRequesting : Boolean) {
        if (isRequesting) {
            // loading now...
            loadingBaseView!!.setBackgroundResource(android.R.color.transparent);
            loadingView!!.animation = rotateAnimation;
            setOnClickListener(null);
        } else {
            // not loading now...
            loadingBaseView!!.setBackgroundResource(selectableBackgroundId);
            loadingView!!.animation = null;

            setOnClickListener {
                setIsRequesting(true)
                // unset loading callback and background
                loadingViewListener?.onReadMoreClicked();
            }
        }
    }

    public interface LoadingViewListener {
        fun onReadMoreClicked()
    }
}
