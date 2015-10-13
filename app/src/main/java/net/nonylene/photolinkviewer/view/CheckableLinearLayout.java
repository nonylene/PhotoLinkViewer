package net.nonylene.photolinkviewer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private CheckedTextView checkedTextView;

    public CheckableLinearLayout(Context context) {
        super(context);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int child = getChildCount();
        for (int i = 0; i < child; i++) {
            View v = getChildAt(i);
            if (v instanceof CheckedTextView) {
                checkedTextView = (CheckedTextView) v;
            }
        }
    }

    @Override
    public boolean isChecked() {
        return checkedTextView != null && checkedTextView.isChecked();
    }

    @Override
    public void setChecked(boolean check) {
        if (checkedTextView != null) {
            checkedTextView.setChecked(check);
        }
    }

    @Override
    public void toggle() {
        if (checkedTextView != null) {
            checkedTextView.toggle();
        }
    }

}
