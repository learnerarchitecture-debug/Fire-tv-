package com.aurora.home;

/** A single curated movie suggestion. All artwork is generated from these colours. */
public final class Movie {
    public final String id;
    public final String title;
    public final String year;
    public final String genre;
    public final String duration;
    public final String rating;
    public final String description;
    public final String tag;
    public final int match;
    public final int c1;
    public final int c2;
    public final int c3;

    public Movie(String id, String title, String year, String genre, String duration,
                 String rating, int match, String description, String tag,
                 int c1, int c2, int c3) {
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
    }

    public String meta() {
        return year + "  ·  " + genre + "  ·  " + duration + "  ·  ★ " + rating;
    }
}
