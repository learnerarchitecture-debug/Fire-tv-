package com.aurora.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

/**
 * Full-screen app picker for the drawer. It lists every launchable package,
 * including Fire OS system apps, and persists the chosen packages. Saving
 * the automatic default set clears the custom flag so newly installed apps
 * continue to appear in the tray automatically.
 */
public class AppPickerActivity extends Activity {

    private TvGridView mGrid;
    private TextView mCount;
    private View mEmpty;
    private View mSave;
    private View mReset;
    private AppGridAdapter mAdapter;
    private IconLoader mLoader;
    private final Handler mUi = new Handler(Looper.getMainLooper());
    private Set<String> mSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.immersive(this);
        setContentView(R.layout.activity_app_picker);

        AuroraBackgroundView bg = (AuroraBackgroundView) findViewById(R.id.picker_bg);
        bg.setTheme(Prefs.theme(this), Prefs.accent(this));

        mGrid = (TvGridView) findViewById(R.id.picker_grid);
        mCount = (TextView) findViewById(R.id.picker_count);
        mEmpty = findViewById(R.id.picker_empty);
        mSave = findViewById(R.id.picker_save);
        mReset = findViewById(R.id.picker_reset);
        mSelected = AppCatalog.visiblePackages(this);

        IconView close = (IconView) findViewById(R.id.picker_close_icon);
        close.setMode(IconView.CLOSE);
        close.setColor(0xFFFFFFFF, 0);
        findViewById(R.id.picker_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        IconView resetIcon = (IconView) findViewById(R.id.picker_reset_icon);
        resetIcon.setMode(IconView.REFRESH);
        resetIcon.setColor(Prefs.accent(this), 0);
        mReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelected = AppCatalog.defaultPackages(AppPickerActivity.this);
                mAdapter.setSelected(mSelected);
                updateCount();
                toast("Default tray restored — press Save to apply");
            }
        });

        IconView saveIcon = (IconView) findViewById(R.id.picker_save_icon);
        saveIcon.setMode(IconView.CHECK);
        saveIcon.setColor(0xFF12151C, 0);
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        mGrid.setEdgeListener(new TvGridView.EdgeListener() {
            @Override
            public void onDownEdge() {
                mSave.requestFocus();
            }

            @Override
            public void onUpEdge() {
                findViewById(R.id.picker_close).requestFocus();
            }
        });

        mSave.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DPAD_UP
                        && mAdapter.getCount() > 0) {
                    mGrid.focusLast();
                    return true;
                }
                return false;
            }
        });
        mReset.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DPAD_UP
                        && mAdapter.getCount() > 0) {
                    mGrid.focusLast();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.picker_close).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                        && mAdapter.getCount() > 0) {
                    mGrid.focusFirst();
                    return true;
                }
                return false;
            }
        });

        mLoader = new IconLoader(this, 128);
        mAdapter = new AppGridAdapter(this, mLoader);
        mAdapter.setSelectMode(true);
        mAdapter.setSelected(mSelected);
        mAdapter.setListener(new AppGridAdapter.Listener() {
            @Override
            public void onAppClick(AppGridAdapter.AppEntry entry) {
                if (entry == null) return;
                mAdapter.toggleSelected(entry.pkg);
                mSelected = mAdapter.getSelected();
                mAdapter.setSelected(mSelected);
                updateCount();
            }
        });
        mGrid.setAdapter(mAdapter);
        updateCount();
        loadApps();
    }

    private void loadApps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppGridAdapter.AppEntry> apps = AppCatalog.all(AppPickerActivity.this, true);
                mUi.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        bindApps(apps);
                    }
                });
            }
        }, "app-picker").start();
    }

    private void bindApps(List<AppGridAdapter.AppEntry> apps) {
        mAdapter.setApps(apps);
        mAdapter.setSelected(mSelected);
        updateCount();
        if (apps.isEmpty()) {
            mGrid.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
            findViewById(R.id.picker_close).requestFocus();
            return;
        }
        mGrid.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mGrid.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && mAdapter.getCount() > 0) mGrid.focusFirst();
            }
        }, 60);
    }

    private void updateCount() {
        mCount.setText(mSelected.size() + " selected");
    }

    private void save() {
        Set<String> defaults = AppCatalog.defaultPackages(this);
        if (mSelected.equals(defaults)) {
            AppCatalog.reset(this);
        } else {
            AppCatalog.save(this, mSelected);
        }
        setResult(RESULT_OK, new Intent());
        toast("App tray updated");
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.immersive(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUi.removeCallbacksAndMessages(null);
        if (mLoader != null) mLoader.destroy();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
