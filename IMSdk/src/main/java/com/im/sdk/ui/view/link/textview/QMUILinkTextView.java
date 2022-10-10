package com.im.sdk.ui.view.link.textview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.im.sdk.R;
import com.im.sdk.ui.view.link.alpha.QMUIAlphaTextView;
import com.im.sdk.ui.view.link.span.QMUIOnSpanClickListener;
import com.im.sdk.ui.view.link.util.QMUILinkTouchMovementMethod;
import com.im.sdk.ui.view.link.util.QMUILinkify;

import java.util.HashSet;
import java.util.Set;


public class QMUILinkTextView extends QMUIAlphaTextView implements QMUIOnSpanClickListener {
    private static final String TAG = "LinkTextView";
    private static final int MSG_CHECK_DOUBLE_TAP_TIMEOUT = 1000;
    public static int AUTO_LINK_MASK_REQUIRED = QMUILinkify.PHONE_NUMBERS | QMUILinkify.EMAIL_ADDRESSES | QMUILinkify.WEB_URLS;
    private static Set<String> AUTO_LINK_SCHEME_INTERRUPTED = new HashSet<>();
    private CharSequence mOriginText = null;

    static {
//        AUTO_LINK_SCHEME_INTERRUPTED.add("tel");
//        AUTO_LINK_SCHEME_INTERRUPTED.add("mailto");
        AUTO_LINK_SCHEME_INTERRUPTED.add("http");
        AUTO_LINK_SCHEME_INTERRUPTED.add("https");
    }

    /**
     * 链接文字颜色
     */
    private ColorStateList mLinkTextColor;
    /**
     * 链接背景颜色
     */
    private ColorStateList mLinkBgColor;

    private int mAutoLinkMaskCompat;
    private OnLinkClickListener mOnLinkClickListener;
    private OnLinkLongClickListener mOnLinkLongClickListener;

    private long mDownMillis = 0;
    private static final long TAP_TIMEOUT = 200; // ViewConfiguration.getTapTimeout();
    private static final long DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

    public QMUILinkTextView(Context context) {
        this(context, null);
        mLinkBgColor = null;
        mLinkTextColor = ContextCompat.getColorStateList(context, R.color.text_00C6DB);
    }

    public QMUILinkTextView(Context context, ColorStateList linkTextColor, ColorStateList linkBgColor) {
        this(context, null);
        mLinkBgColor = linkBgColor;
        mLinkTextColor = linkTextColor;
    }

    public QMUILinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAutoLinkMaskCompat = getAutoLinkMask() | AUTO_LINK_MASK_REQUIRED;
        setAutoLinkMask(0);
        setMovementMethodCompat(QMUILinkTouchMovementMethod.getInstance());
        setHighlightColor(Color.TRANSPARENT);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.QMUILinkTextView);
        mLinkBgColor = array.getColorStateList(R.styleable.QMUILinkTextView_qmui_linkBackgroundColor);
        mLinkTextColor = array.getColorStateList(R.styleable.QMUILinkTextView_qmui_linkTextColor);
        array.recycle();
        if (mOriginText != null) {
            setText(mOriginText);
        }
        setChangeAlphaWhenPress(false);
    }

    public void setOnLinkClickListener(OnLinkClickListener onLinkClickListener) {
        mOnLinkClickListener = onLinkClickListener;
    }

    public void setOnLinkLongClickListener(OnLinkLongClickListener onLinkLongClickListener) {
        mOnLinkLongClickListener = onLinkLongClickListener;
    }

    public int getAutoLinkMaskCompat() {
        return mAutoLinkMaskCompat;
    }

    public void setAutoLinkMaskCompat(int mask) {
        mAutoLinkMaskCompat = mask;
    }

    public void addAutoLinkMaskCompat(int mask) {
        mAutoLinkMaskCompat |= mask;
    }

    public void removeAutoLinkMaskCompat(int mask) {
        mAutoLinkMaskCompat &= ~mask;
    }


    public void setLinkColor(ColorStateList linkTextColor) {
        mLinkTextColor = linkTextColor;
    }

    @Override
    public void setText(CharSequence text, TextView.BufferType type) {
        mOriginText = text;
        if (!TextUtils.isEmpty(text)) {
            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            QMUILinkify.addLinks(builder, mAutoLinkMaskCompat, mLinkTextColor, mLinkBgColor, this);
            text = builder;
        }
        super.setText(text, type);
    }

    @Override
    public boolean onSpanClick(String text) {
        if (null == text) {
            Log.w(TAG, "onSpanClick interrupt null text");
            return true;
        }
        long clickUpTime = (SystemClock.uptimeMillis() - mDownMillis);
        Log.w(TAG, "onSpanClick clickUpTime: " + clickUpTime);
        if (mSingleTapConfirmedHandler.hasMessages(MSG_CHECK_DOUBLE_TAP_TIMEOUT)) {
            disallowOnSpanClickInterrupt();
            return true;
        }

        if (TAP_TIMEOUT < clickUpTime) {
            Log.w(TAG, "onSpanClick interrupted because of TAP_TIMEOUT: " + clickUpTime);
            return true;
        }

        String scheme = Uri.parse(text).getScheme();
        if (scheme != null) {
            scheme = scheme.toLowerCase();
        }

        if (AUTO_LINK_SCHEME_INTERRUPTED.contains(scheme)) {
            long waitTime = DOUBLE_TAP_TIMEOUT - clickUpTime;
            mSingleTapConfirmedHandler.removeMessages(MSG_CHECK_DOUBLE_TAP_TIMEOUT);
            Message msg = Message.obtain();
            msg.what = MSG_CHECK_DOUBLE_TAP_TIMEOUT;
            msg.obj = text;
            mSingleTapConfirmedHandler.sendMessageDelayed(msg, waitTime);
            return true;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                boolean hasSingleTap = mSingleTapConfirmedHandler.hasMessages(MSG_CHECK_DOUBLE_TAP_TIMEOUT);
                Log.w(TAG, "onTouchEvent hasSingleTap: " + hasSingleTap);
                if (!hasSingleTap) {
                    mDownMillis = SystemClock.uptimeMillis();
                } else {
                    Log.w(TAG, "onTouchEvent disallow onSpanClick mSingleTapConfirmedHandler because of DOUBLE TAP");
                    disallowOnSpanClickInterrupt();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void disallowOnSpanClickInterrupt() {
        mSingleTapConfirmedHandler.removeMessages(MSG_CHECK_DOUBLE_TAP_TIMEOUT);
        mDownMillis = 0;
    }

    protected boolean performSpanLongClick(String text) {
        if (mOnLinkLongClickListener != null) {
            mOnLinkLongClickListener.onLongClick(text);
            return true;
        }
        return false;
    }


    @Override
    public boolean performLongClick() {
        //do not need long click
        int end = getSelectionEnd();

        if (end > 0) {

            String selectStr = getText().subSequence(getSelectionStart(), end).toString();
            return performSpanLongClick(selectStr) || super.performLongClick();

        }
        return super.performLongClick();
    }

    private Handler mSingleTapConfirmedHandler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(Message msg) {
            if (MSG_CHECK_DOUBLE_TAP_TIMEOUT != msg.what) {
                return;
            }

            Log.d(TAG, "handleMessage: " + msg.obj);
            if (msg.obj instanceof String) {
                String url = (String) msg.obj;
                if (null != mOnLinkClickListener && !TextUtils.isEmpty(url)) {
                    String schemeUrl = url.toLowerCase();
                    if (schemeUrl.startsWith("tel:")) {
                        String phoneNumber = Uri.parse(url).getSchemeSpecificPart();
                        mOnLinkClickListener.onTelLinkClick(phoneNumber);
                    } else if (schemeUrl.startsWith("mailto:")) {
                        String mailAddr = Uri.parse(url).getSchemeSpecificPart();
                        mOnLinkClickListener.onMailLinkClick(mailAddr);
                    } else if (schemeUrl.startsWith("http") || schemeUrl.startsWith("https")) {
                        mOnLinkClickListener.onWebUrlLinkClick(url);
                    }
                }
            }
        }

    };



    public interface OnLinkClickListener {

        /**
         * phone click
         */
        void onTelLinkClick(String phoneNumber);

        /**
         * e-mail click
         */
        void onMailLinkClick(String mailAddress);

        /**
         * URL click
         */
        void onWebUrlLinkClick(String url);

    }

    public interface OnLinkLongClickListener {
        void onLongClick(String text);
    }
}
