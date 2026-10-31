package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        View check;
        IconView checkIcon;
        AppEntry entry;
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
    private final Set<String> mSelected = new HashSet<String>();
    private boolean mSelectMode;
    private Listener mListener;

    private final View.OnFocusChangeListener mFocusL = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            ViewHolder h = (ViewHolder) v.getTag();
            if (h == null) return;
            boolean selected = mSelectMode && h.entry != null
                    && mSelected.contains(h.entry.pkg);
            if (hasFocus) {
                if (mAnimateFocus) {
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(90).start();
                } else {
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
            } else {
                if (mAnimateFocus) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                } else {
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
            }
            h.ring.setVisibility(hasFocus || selected ? View.VISIBLE : View.INVISIBLE);
            h.label.setTextColor(hasFocus || selected ? mAccent : mLabelColor);
            h.label.setSelected(hasFocus);
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

    public void setSelectMode(boolean selectMode) {
        mSelectMode = selectMode;
        notifyDataSetChanged();
    }

    public void setSelected(Set<String> packages) {
        mSelected.clear();
        if (packages != null) mSelected.addAll(packages);
        notifyDataSetChanged();
    }

    public boolean isSelected(String pkg) {
        return mSelected.contains(pkg);
    }

    public void toggleSelected(String pkg) {
        if (pkg == null) return;
        if (!mSelected.remove(pkg)) mSelected.add(pkg);
    }

    public Set<String> getSelected() {
        return new HashSet<String>(mSelected);
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
            h.check = v.findViewById(R.id.check);
            h.checkIcon = (IconView) v.findViewById(R.id.check_icon);
            v.setTag(h);
            v.setOnFocusChangeListener(mFocusL);
        } else {
            h = (ViewHolder) v.getTag();
        }

        final AppEntry e = mApps.get(position);
        h.entry = e;
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

        boolean selected = mSelectMode && mSelected.contains(e.pkg);
        h.check.setVisibility(selected ? View.VISIBLE : View.GONE);
        if (selected) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(mAccent);
            h.check.setBackgroundDrawable(dot);
            h.checkIcon.setMode(IconView.CHECK);
            h.checkIcon.setColor(0xFF12151C, 0);
        }
        h.ring.setVisibility(v.isFocused() || selected ? View.VISIBLE : View.INVISIBLE);
        h.label.setTextColor(v.isFocused() || selected ? mAccent : mLabelColor);

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) mListener.onAppClick(e);
            }
        });
        return v;
    }
}
