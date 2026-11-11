package com.aurora.home;

/** A curated movie suggestion: procedural palette data plus real artwork
 *  fetched asynchronously by ThumbnailLoader (iTunes Search, disk cached). */
public final class Movie {
    public final String id;
    public final String title;
    public final String year;
    public final String genre;
    public final String duration;
    public final String rating;
    public final String description;
    public final String tag;
    /** Artwork bitmap fetched by ThumbnailLoader. Volatile: the art pool
     *  thread writes it while UI threads read it during draw. */
    public volatile android.graphics.Bitmap thumbnail;
    public final int match;
    public final int c1;
    public final int c2;
    public final int c3;
    /** Installed OTT provider that supplies this recommendation. */
    public final String providerName;
    public final String providerPkg;
    /** Keyless deep-link target: "yt" (YouTube video id) or "nf" (Netflix
     *  title id). Empty when no verified link exists — Open then falls
     *  back to launching the provider app. */
    public final String linkKind;
    public final String videoId;

    public Movie(String id, String title, String year, String genre, String duration,
                 String rating, int match, String description, String tag,
                 int c1, int c2, int c3) {
        this(id, title, year, genre, duration, rating, match, description, tag,
                c1, c2, c3, "", "", "", "");
    }

    public Movie(String id, String title, String year, String genre, String duration,
                 String rating, int match, String description, String tag,
                 int c1, int c2, int c3, String providerName, String providerPkg) {
        this(id, title, year, genre, duration, rating, match, description, tag,
                c1, c2, c3, providerName, providerPkg, "", "");
    }

    public Movie(String id, String title, String year, String genre, String duration,
                 String rating, int match, String description, String tag,
                 int c1, int c2, int c3, String providerName, String providerPkg,
                 String linkKind, String videoId) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.genre = genre;
        this.duration = duration;
        this.rating = rating;
        this.match = match;
        this.description = description;
        this.tag = tag;
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.providerName = providerName == null ? "" : providerName;
        this.providerPkg = providerPkg == null ? "" : providerPkg;
        this.linkKind = linkKind == null ? "" : linkKind;
        this.videoId = videoId == null ? "" : videoId;
        // Real artwork is fetched asynchronously from the views via
        // ThumbnailLoader.load(...); the procedural poster is the fallback.
    }

    public String meta() {
        return year + "  ·  " + genre + "  ·  " + duration + "  ·  ★ " + rating;
    }
}
