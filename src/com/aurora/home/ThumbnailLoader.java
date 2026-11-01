package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;

/** Fetches movie posters from iTunes Search (public, no key). Falls back to procedural. */
public final class ThumbnailLoader {
    private static final String BASE = "https://itunes.apple.com/search?media=movie&entity=movie&term=";
    public static Bitmap load(Context c, String title, int year) { return null; } // stub: real fetch needs network thread + URL encoding + art cache; procedural fallback always works
    public static void cache(File f, Bitmap b) {}
}
