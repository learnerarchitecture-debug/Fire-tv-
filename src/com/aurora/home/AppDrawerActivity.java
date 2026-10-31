package com.aurora.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Bottom-sheet app drawer for the home screen. The automatic tray contains
 * every launchable user app plus YouTube, Netflix and Prime Video. The
 * "Add apps" button opens a picker that can also add Fire OS system apps;
 * the chosen packages are stored in Prefs.
 */
public class AppDrawerActivity extends Activity {

    private static final int REQ_PICK_APPS = 41;

    private TvGridView mGrid;
    private TextView mSubtitle;
    private View mSheet;
    private View mScrim;
    private View mEmpty;
    private View mAddApps;
    private AppGridAdapter mAdapter;
    private IconLoader mLoader;
    private final Handler mUi = new Handler(Looper.getMainLooper());
    private boolean mClosing;
    private boolean mFirstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.immersive(this);
        setContentView(R.layout.activity_app_drawer);

        mSheet = findViewById(R.id.drawer_sheet);
        mScrim = findViewById(R.id.drawer_scrim);
        mGrid = (TvGridView) findViewById(R.id.drawer_grid);
        mEmpty = findViewById(R.id.drawer_empty);
        mSubtitle = (TextView) findViewById(R.id.drawer_subtitle);
        mAddApps = findViewById(R.id.drawer_add);

        IconView closeIcon = (IconView) findViewById(R.id.drawer_close_icon);
        closeIcon.setMode(IconView.CLOSE);
        closeIcon.setColor(0xFFFFFFFF, 0);
        findViewById(R.id.drawer_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDrawer();
            }
        });

        IconView addIcon = (IconView) findViewById(R.id.drawer_add_icon);
        addIcon.setMode(IconView.PLUS);
        addIcon.setColor(Prefs.accent(this), 0);
        mAddApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPicker();
            }
        });
        mAddApps.setOnKeyListener(new View.OnKeyListener() {
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

        mScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDrawer();
            }
        });

        mGrid.setEdgeListener(new TvGridView.EdgeListener() {
            @Override
            public void onDownEdge() {
                mAddApps.requestFocus();
            }

            @Override
            public void onUpEdge() {
                findViewById(R.id.drawer_close).requestFocus();
            }
        });

        mLoader = new IconLoader(this, 128);
        mAdapter = new AppGridAdapter(this, mLoader);
        mAdapter.setListener(new AppGridAdapter.Listener() {
            @Override
            public void onAppClick(AppGridAdapter.AppEntry entry) {
                openApp(entry);
            }
        });
        mGrid.setAdapter(mAdapter);

        animateIn();
        loadApps();
    }

    private void animateIn() {
        mScrim.setAlpha(0f);
        if (Prefs.reduceMotion(this)) {
            mScrim.setAlpha(1f);
            return;
        }
        mSheet.setVisibility(View.INVISIBLE);
        mSheet.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (mSheet.getHeight() <= 0) return true;
                        mSheet.getViewTreeObserver().removeOnPreDrawListener(this);
                        mSheet.setTranslationY(mSheet.getHeight());
                        mSheet.setVisibility(View.VISIBLE);
                        mSheet.animate()
                                .translationY(0f)
                                .setDuration(220)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                        mScrim.animate()
                                .alpha(1f)
                                .setDuration(220)
                                .start();
                        return true;
                    }
                });
    }

    private void closeDrawer() {
        if (mClosing) return;
        mClosing = true;
        if (Prefs.reduceMotion(this)) {
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        mSheet.animate()
                .translationY(mSheet.getHeight())
                .setDuration(180)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
        mScrim.animate().alpha(0f).setDuration(180).start();
    }

    private void loadApps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppGridAdapter.AppEntry> apps = AppCatalog.visible(AppDrawerActivity.this);
                mUi.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        bindApps(apps);
                    }
                });
            }
        }, "app-drawer").start();
    }

    private void bindApps(List<AppGridAdapter.AppEntry> apps) {
        mAdapter.setApps(apps);
        String lead = AppCatalog.isCustom(this)
                ? "YOUR TRAY" : getString(R.string.apps_lead);
        mSubtitle.setText(lead + " \u00B7 " + apps.size());
        if (apps.isEmpty()) {
            mGrid.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
            mAddApps.requestFocus();
            return;
        }
        mGrid.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mGrid.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || mAdapter.getCount() == 0) return;
                mGrid.focusFirst();
            }
        }, 60);
    }

    private void openPicker() {
        startActivityForResult(new Intent(this, AppPickerActivity.class), REQ_PICK_APPS);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFirstResume) {
            mFirstResume = false;
            return;
        }
        loadApps();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_APPS) {
            loadApps();
            mAddApps.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAddApps.requestFocus();
                }
            }, 120);
        }
    }

    private void openApp(AppGridAdapter.AppEntry entry) {
        if (entry == null || entry.intent == null) return;
        try {
            Intent intent = new Intent(entry.intent);
            // Keep the app above OBAA in the same task where possible so Back
            // returns to the launcher rather than the Fire TV stock home.
            intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Throwable t) {
            Toast.makeText(this, "Could not open " + entry.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        closeDrawer();
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
}
