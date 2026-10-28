package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AppGridAdapter extends BaseAdapter {

    static class AppEntry {
        CharSequence label;
        String pkg;
        String clz;
        android.content.Intent intent;
    }

    static class ViewHolder {
        ImageView icon;
        TextView label;
        View ring;
    }

    public interface Listener {
        void onAppClick(AppEntry entry);
    }

    private final Context mCtx;
    private final LayoutInflater mInflater;
    private final IconLoader mLoader;
    private final List<AppEntry> mApps = new ArrayList<AppEntry>();
    private final int mAccent;
    private final int mLabelColor;
    private final float mRadiusPx;
    private final boolean mAnimateFocus;
    private Listener mListener;

    private final View.OnFocusChangeListener mFocusL = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            ViewHolder h = (ViewHolder) v.getTag();
            if (h == null) return;
            if (hasFocus) {
                if (mAnimateFocus) {
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(90).start();
                } else {
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
                h.ring.setVisibility(View.VISIBLE);
                h.label.setTextColor(mAccent);
                h.label.setSelected(true);
            } else {
                if (mAnimateFocus) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                } else {
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
                h.ring.setVisibility(View.INVISIBLE);
                h.label.setTextColor(mLabelColor);
                h.label.setSelected(false);
            }
        }
    };

    public AppGridAdapter(Context ctx, IconLoader loader) {
        mCtx = ctx;
        mLoader = loader;
        mInflater = LayoutInflater.from(ctx);
        mAccent = Prefs.accent(ctx);
        mLabelColor = 0xFFC4CDDB;
        mRadiusPx = ctx.getResources().getDimensionPixelSize(R.dimen.drawer_icon_radius);
        mAnimateFocus = !Prefs.reduceMotion(ctx);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setApps(List<AppEntry> apps) {
        mApps.clear();
        mApps.addAll(apps);
        notifyDataSetChanged();
    }

    public AppEntry get(int position) {
        return mApps.get(position);
    }

    @Override
    public int getCount() {
        return mApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder h;
        if (v == null) {
            v = mInflater.inflate(R.layout.item_app, parent, false);
            h = new ViewHolder();
            h.icon = (ImageView) v.findViewById(R.id.icon);
            h.label = (TextView) v.findViewById(R.id.label);
            h.ring = v.findViewById(R.id.ring);
            v.setTag(h);
            v.setOnFocusChangeListener(mFocusL);
        } else {
            h = (ViewHolder) v.getTag();
        }

        final AppEntry e = mApps.get(position);
        h.label.setText(e.label);
        h.icon.setTag(e.pkg);

        android.graphics.drawable.Drawable d = h.icon.getDrawable();
        if (!(d instanceof RoundedIconDrawable)) {
            d = new RoundedIconDrawable(mRadiusPx);
            h.icon.setImageDrawable(d);
        }
        Bitmap b = mLoader.peek(e.pkg);
        ((RoundedIconDrawable) d).setBitmap(b);
        if (b == null) {
            mLoader.queue(e.pkg, h.icon);
        }
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) mListener.onAppClick(e);
            }
        });
        return v;
    }
}
