package com.aurora.home;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds recommendations from the OTT apps actually installed on the Fire TV.
 * The list rotates through a window of titles every five minutes; the chosen
 * provider package is carried on every Movie so "Open" launches the right app.
 */
public final class Recommendations {

    public static final long ROTATE_MS = 5L * 60L * 1000L;
    private static final int WINDOW = 10;

    private static final class Title {
        final String id, title, year, genre, duration, rating, description, tag;
        final int match;

        Title(String id, String title, String year, String genre, String duration,
              String rating, int match, String description, String tag) {
            this.id = id;
            this.title = title;
            this.year = year;
            this.genre = genre;
            this.duration = duration;
            this.rating = rating;
            this.match = match;
            this.description = description;
            this.tag = tag;
        }
    }

    private static final class Provider {
        final String key, name;
        final String[] packages;
        final int c1, c2, c3;
        final List<Title> titles = new ArrayList<Title>();

        Provider(String key, String name, int c1, int c2, int c3, String[] packages) {
            this.key = key;
            this.name = name;
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.packages = packages;
        }
    }

    private static final List<Provider> PROVIDERS = new ArrayList<Provider>();

    static {
        provider("prime", "Prime Video", 0xFF1F6FEB, 0xFF07162E, 0xFF7FB7FF,
                new String[]{"com.amazon.avod", "com.amazon.amazonvideo.livingroom"},
                t("dune2", "Dune: Part Two", "2024", "Sci-Fi · Epic", "2h 46m",
                        "8.8", 98, "Paul Atreides unites with the Fremen and walks the path of revenge against the conspirators who destroyed his family.", "TRENDING"),
                t("theboys", "The Boys", "2019", "Action · Satire", "4 seasons",
                        "8.7", 96, "A group of vigilantes sets out to take down corrupt superheroes who abuse their powers.", "TOP 10"),
                t("mirzapur", "Mirzapur", "2018", "Crime · Drama", "3 seasons",
                        "8.5", 94, "Power, politics and crime collide in the lawless heartland of Mirzapur.", "ORIGINAL"),
                t("fallout", "Fallout", "2024", "Sci-Fi · Adventure", "1 season",
                        "8.4", 93, "Two hundred years after the apocalypse, a vault dweller steps into a strange and violent wasteland.", "NEW"),
                t("familyman", "The Family Man", "2019", "Action · Thriller", "2 seasons",
                        "8.7", 95, "A middle-class man secretly works as an intelligence officer protecting the nation.", "ORIGINAL"));

        provider("netflix", "Netflix", 0xFFE50914, 0xFF260406, 0xFFFF8A80,
                new String[]{"com.netflix.mediaclient", "com.netflix.ninja"},
                t("stranger", "Stranger Things", "2016", "Sci-Fi · Horror", "4 seasons",
                        "8.7", 97, "A small town uncovers a mystery involving secret experiments and a girl with strange powers.", "TOP 10"),
                t("wednesday", "Wednesday", "2022", "Mystery · Comedy", "1 season",
                        "8.1", 94, "Wednesday Addams investigates a monstrous killing spree at Nevermore Academy.", "TRENDING"),
                t("extraction2", "Extraction 2", "2023", "Action · Thriller", "2h 3m",
                        "7.0", 90, "A black-market mercenary returns for another deadly rescue mission.", "ACTION"),
                t("crown", "The Crown", "2016", "Drama · History", "6 seasons",
                        "8.6", 95, "The political and personal story of the British royal family across the decades.", "AWARD WINNER"),
                t("squidgame", "Squid Game", "2021", "Thriller · Drama", "1 season",
                        "8.0", 93, "Hundreds of cash-strapped players accept a strange invitation to compete in children's games.", "BINGE"));

        provider("airtel", "Airtel Xstream Play", 0xFFE4002B, 0xFF240006, 0xFFFF6B81,
                new String[]{"tv.airtel.xstream.tvapp", "tv.airtel.xstream",
                        "com.airtel.xstream", "com.airtel.xstreamplay", "com.airtel.tv"},
                t("jawan", "Jawan", "2023", "Action · Thriller", "2h 49m",
                        "8.1", 95, "A man is driven by a personal vendetta to rectify the wrongs in society.", "BLOCKBUSTER"),
                t("pathaan", "Pathaan", "2023", "Action · Spy", "2h 26m",
                        "7.9", 93, "An exiled spy returns to stop a rogue outfit from unleashing a deadly attack.", "TRENDING"),
                t("kota", "Kota Factory", "2019", "Drama · Coming of age", "3 seasons",
                        "8.6", 94, "Students navigate friendship, pressure and ambition at a coaching institute in Kota.", "BINGE"),
                t("gullak", "Gullak", "2019", "Comedy · Family", "4 seasons",
                        "8.7", 96, "Warm, funny stories about a middle-class family and their everyday battles.", "FEEL GOOD"),
                t("kapil", "The Kapil Sharma Show", "2016", "Comedy · Talk", "3 seasons",
                        "8.0", 90, "Celebrity guests, sketches and stand-up from one of India's biggest comedy stages.", "LAUGH"));

        provider("hotstar", "Disney+ Hotstar", 0xFF1F80E0, 0xFF06152B, 0xFF6CC4FF,
                new String[]{"in.startv.hotstar", "in.startv.hotstar.dplus", "com.disney.disneyplus"},
                t("loki", "Loki", "2021", "Sci-Fi · Adventure", "2 seasons",
                        "8.2", 95, "The God of Mischief steps out of his brother's shadow and into a time-bending adventure.", "MARVEL"),
                t("koffee", "Koffee with Karan", "2004", "Talk · Celebrity", "8 seasons",
                        "7.8", 88, "Bollywood stars spill stories, gossip and secrets over coffee.", "CHAT"),
                t("specialops", "Special Ops", "2020", "Action · Thriller", "2 seasons",
                        "8.0", 92, "An intelligence officer hunts the mastermind behind a series of terror attacks.", "ORIGINAL"),
                t("dhoni", "MS Dhoni: The Untold Story", "2016", "Biography · Sport", "3h 10m",
                        "8.0", 93, "The life and career of one of India's most beloved cricket captains.", "SPORT"),
                t("mandalorian", "The Mandalorian", "2019", "Sci-Fi · Western", "3 seasons",
                        "8.6", 96, "A lone bounty hunter travels the outer reaches of the galaxy with a mysterious child.", "STAR WARS"));

        provider("sonyliv", "SonyLIV", 0xFF6D4AFF, 0xFF100A2A, 0xFFB9A8FF,
                new String[]{"com.sonyliv"},
                t("scam1992", "Scam 1992", "2020", "Drama · Finance", "1 season",
                        "9.2", 99, "The rise and fall of stockbroker Harshad Mehta and the 1992 securities scam.", "TOP RATED"),
                t("rocketboys", "Rocket Boys", "2022", "Drama · History", "2 seasons",
                        "8.7", 96, "The story of India's pioneering scientists and the nation's early space programme.", "AWARD WINNER"),
                t("nightmanager", "The Night Manager", "2023", "Thriller · Spy", "1 season",
                        "8.1", 93, "A hotel night manager is drawn into the dangerous world of arms dealing.", "THRILLER"),
                t("sharktank", "Shark Tank India", "2021", "Reality · Business", "3 seasons",
                        "7.9", 89, "Founders pitch their ideas to a panel of India's sharpest investors.", "REALITY"));

        provider("zee5", "ZEE5", 0xFF9B51E0, 0xFF1A0726, 0xFFE0A8FF,
                new String[]{"com.zee5.zee5", "com.zee5.androidtv", "com.zee5"},
                t("kashmirfiles", "The Kashmir Files", "2022", "Drama · History", "2h 50m",
                        "7.7", 91, "A retired officer searches for the truth about the exodus of Kashmiri Pandits.", "DRAMA"),
                t("taj", "Taj: Divided by Blood", "2023", "Drama · History", "2 seasons",
                        "7.5", 89, "A Mughal emperor's sons battle for the throne in a story of ambition and betrayal.", "ORIGINAL"),
                t("sunflower", "Sunflower", "2021", "Mystery · Comedy", "2 seasons",
                        "7.6", 90, "A middle-class man becomes the prime suspect in a murder in his housing society.", "BINGE"),
                t("rrr", "RRR", "2022", "Action · Epic", "3h 7m",
                        "8.0", 94, "Two revolutionaries forge an unlikely friendship in pre-independence India.", "EPIC"));

        provider("jio", "JioCinema", 0xFFE11D48, 0xFF26020A, 0xFFFF7A9A,
                new String[]{"com.jio.media.jiobeats", "com.jio.cinema", "com.jio.media.jiocinema"},
                t("biggboss", "Bigg Boss", "2006", "Reality · Drama", "17 seasons",
                        "6.5", 86, "Housemates live together under constant surveillance while competing for the trophy.", "REALITY"),
                t("animal", "Animal", "2023", "Action · Crime", "3h 21m",
                        "7.8", 92, "A son's obsessive love for his father spirals into a violent path.", "BLOCKBUSTER"),
                t("ramsetu", "Ram Setu", "2022", "Adventure · Mystery", "2h 30m",
                        "6.8", 87, "An archaeologist sets out to prove the existence of a legendary bridge.", "ADVENTURE"),
                t("special26", "Special 26", "2013", "Crime · Thriller", "2h 24m",
                        "7.9", 92, "A group of con artists poses as tax officials to pull off a daring heist.", "CRIME"));

        provider("youtube", "YouTube", 0xFFFF0000, 0xFF260000, 0xFFFF7A7A,
                new String[]{"com.amazon.firetv.youtube", "com.google.android.youtube.tv", "com.google.android.youtube"},
                t("trending", "Trending Now", "2026", "Viral · Music", "24/7",
                        "9.1", 97, "The videos, songs and moments everyone is watching right now.", "TRENDING"),
                t("mkbhd", "The Future of TV", "2026", "Tech · Review", "18m",
                        "8.9", 95, "A close look at the displays, chips and ideas shaping the next generation of television.", "TECH"),
                t("kurzgesagt", "The Universe, Explained", "2026", "Science · Animation", "12m",
                        "9.0", 96, "A beautifully animated journey through the biggest questions in astrophysics.", "SCIENCE"),
                t("cricket", "Live: Cricket Highlights", "2026", "Sport · Live", "45m",
                        "8.6", 92, "Every six, wicket and turning point from the latest match.", "LIVE"));

        provider("mx", "MX Player", 0xFFFF7A00, 0xFF2A1000, 0xFFFFC46B,
                new String[]{"com.mxtech.videoplayer.ad"},
                t("aashram", "Aashram", "2020", "Crime · Drama", "3 seasons",
                        "7.6", 91, "A self-styled godman builds an empire while dark secrets gather around him.", "ORIGINAL"),
                t("bhaukaal", "Bhaukaal", "2020", "Crime · Action", "2 seasons",
                        "8.2", 93, "A fearless SSP takes on organised crime in Muzaffarnagar.", "ACTION"),
                t("raktanchal", "Raktanchal", "2020", "Crime · Drama", "2 seasons",
                        "7.4", 89, "Rival families fight for power and contracts in eastern Uttar Pradesh.", "CRIME"));

        provider("appletv", "Apple TV", 0xFF4A4A4A, 0xFF0A0A0A, 0xFFE8E8E8,
                new String[]{"com.apple.atve.androidtv.appletv"},
                t("foundation", "Foundation", "2021", "Sci-Fi · Epic", "2 seasons",
                        "7.5", 91, "A band of exiles fights to save humanity as a galactic empire collapses.", "EPIC"),
                t("severance", "Severance", "2022", "Mystery · Drama", "1 season",
                        "8.7", 96, "Office workers have their memories surgically divided between work and home.", "ORIGINAL"),
                t("silo", "Silo", "2023", "Sci-Fi · Mystery", "1 season",
                        "8.1", 94, "Thousands live underground, forbidden from asking what lies outside.", "MYSTERY"),
                t("tedlasso", "Ted Lasso", "2020", "Comedy · Sport", "3 seasons",
                        "8.8", 97, "An American football coach takes charge of an English football club.", "FEEL GOOD"));

        provider("discovery", "Discovery+", 0xFF1E88E5, 0xFF06152B, 0xFF7FC4FF,
                new String[]{"com.discovery.discoveryplus.androidtv", "com.discovery.discoveryplus"},
                t("goldrush", "Gold Rush", "2010", "Reality · Adventure", "14 seasons",
                        "7.3", 88, "Miners risk everything in the hunt for gold in remote wilderness.", "REALITY"),
                t("deadliest", "Deadliest Catch", "2005", "Reality · Adventure", "20 seasons",
                        "7.8", 90, "Crab fishermen battle brutal weather and dangerous seas.", "ADVENTURE"),
                t("manvswild", "Man vs. Wild", "2006", "Survival · Documentary", "6 seasons",
                        "8.0", 92, "A survival expert is dropped into some of the planet's harshest environments.", "SURVIVAL"));

        provider("lionsgate", "Lionsgate Play", 0xFFC9A227, 0xFF1A1203, 0xFFF5D76E,
                new String[]{"com.lionsgateplay.androidtv", "com.lionsgateplay"},
                t("johnwick", "John Wick", "2014", "Action · Thriller", "1h 41m",
                        "7.4", 92, "A retired hitman returns to the underworld to settle a debt.", "ACTION"),
                t("hungergames", "The Hunger Games", "2012", "Sci-Fi · Adventure", "2h 22m",
                        "7.2", 90, "A young woman volunteers for a televised fight to the death.", "DYSTOPIA"),
                t("knivesout", "Knives Out", "2019", "Mystery · Comedy", "2h 10m",
                        "7.9", 94, "A detective investigates the death of a wealthy crime novelist.", "MYSTERY"));

        provider("aha", "Aha", 0xFFFF6B00, 0xFF2A0D00, 0xFFFFB36B,
                new String[]{"com.aha.aha", "com.aha.tvs"},
                t("bhoothakaalam", "Bhoothakaalam", "2022", "Horror · Drama", "1h 45m",
                        "7.6", 90, "A mother and son confront strange events in their new home.", "HORROR"),
                t("karthikeya2", "Karthikeya 2", "2022", "Adventure · Mystery", "2h 25m",
                        "7.2", 89, "A doctor follows a trail of ancient secrets across India.", "ADVENTURE"),
                t("savethetigers", "Save the Tigers", "2023", "Comedy · Drama", "1 season",
                        "7.8", 91, "Three friends navigate marriage, money and middle-class chaos.", "FEEL GOOD"));

        provider("sunnxt", "Sun NXT", 0xFFE65100, 0xFF2A0A00, 0xFFFFA76B,
                new String[]{"com.sunnxt"},
                t("kantara", "Kantara", "2022", "Action · Fantasy", "2h 28m",
                        "8.3", 95, "A tribal folklore and a divine conflict collide in coastal Karnataka.", "BLOCKBUSTER"),
                t("vikram", "Vikram", "2022", "Action · Thriller", "2h 55m",
                        "8.2", 94, "A special agent investigates a series of murders linked to a shadowy syndicate.", "ACTION"),
                t("kgf2", "KGF: Chapter 2", "2022", "Action · Epic", "2h 48m",
                        "8.0", 93, "A feared gangster expands his empire and faces new enemies.", "EPIC"));

        provider("voot", "Voot", 0xFF6A1B9A, 0xFF1A0526, 0xFFCE93D8,
                new String[]{"com.tv.voot"},
                t("bbott", "Bigg Boss OTT", "2021", "Reality · Drama", "2 seasons",
                        "6.3", 85, "Contestants compete around the clock for the ultimate prize.", "REALITY"),
                t("kyyy", "Kaisi Yeh Yaariaan", "2014", "Drama · Romance", "4 seasons",
                        "7.0", 87, "Friendship and romance collide at a prestigious music college.", "ROMANCE"),
                t("splitsvilla", "Splitsvilla", "2008", "Reality · Romance", "15 seasons",
                        "6.8", 86, "Young singles search for love and survival in a competitive villa.", "REALITY"));

        provider("hoichoi", "Hoichoi", 0xFFD32F2F, 0xFF260606, 0xFFFF8A80,
                new String[]{"com.hoichoi"},
                t("byomkesh", "Byomkesh", "2017", "Mystery · Crime", "5 seasons",
                        "8.2", 93, "A Bengali detective solves intricate crimes with wit and observation.", "MYSTERY"),
                t("feluda", "Feluda", "2018", "Mystery · Adventure", "3 seasons",
                        "8.0", 91, "A sharp investigator and his companions unravel puzzling cases.", "MYSTERY"),
                t("belashuru", "Bela Shuru", "2023", "Drama · Family", "1 season",
                        "7.8", 90, "A family navigates change, love and loss in rural Bengal.", "DRAMA"));
    }

    private Recommendations() {}

    private static Title t(String id, String title, String year, String genre,
                           String duration, String rating, int match,
                           String description, String tag) {
        return new Title(id, title, year, genre, duration, rating, match, description, tag);
    }

    private static void provider(String key, String name, int c1, int c2, int c3,
                                 String[] packages, Title... titles) {
        Provider p = new Provider(key, name, c1, c2, c3, packages);
        p.titles.addAll(Arrays.asList(titles));
        PROVIDERS.add(p);
    }

    /** A rotating window of installed-provider recommendations. */
    public static List<Movie> build(Context context, int rotation) {
        Set<String> installed = installedPackages(context);
        List<Movie> pool = new ArrayList<Movie>();
        for (Provider p : PROVIDERS) {
            String pkg = resolve(p, installed);
            if (pkg == null) continue;
            int i = 0;
            for (Title t : p.titles) {
                int c1 = Ui.mix(p.c1, 0xFF07090F, (i % 4) * 0.10f);
                int c2 = Ui.mix(p.c2, 0xFF05070B, (i % 4) * 0.10f);
                pool.add(new Movie(p.key + "-" + t.id, t.title, t.year, t.genre,
                        t.duration, t.rating, t.match, t.description, t.tag,
                        c1, c2, p.c3, p.name, pkg));
                i++;
            }
        }
        if (pool.isEmpty()) {
            for (Movie m : MovieCatalog.MOVIES) pool.add(copy(m, "OBAA Picks", ""));
        }
        for (int i = 0; pool.size() < WINDOW && i < MovieCatalog.MOVIES.size(); i++) {
            Movie m = MovieCatalog.MOVIES.get(i);
            if (!hasId(pool, m.id)) pool.add(copy(m, "OBAA Picks", ""));
        }
        if (pool.isEmpty()) return pool;

        int n = Math.min(WINDOW, pool.size());
        int offset = ((rotation * 2) % pool.size() + pool.size()) % pool.size();
        List<Movie> out = new ArrayList<Movie>();
        for (int i = 0; i < n; i++) {
            out.add(pool.get((offset + i) % pool.size()));
        }
        return out;
    }

    private static Movie copy(Movie m, String provider, String pkg) {
        return new Movie(m.id, m.title, m.year, m.genre, m.duration, m.rating,
                m.match, m.description, m.tag, m.c1, m.c2, m.c3, provider, pkg);
    }

    private static boolean hasId(List<Movie> list, String id) {
        for (Movie m : list) if (m.id.equals(id)) return true;
        return false;
    }

    private static String resolve(Provider p, Set<String> installed) {
        for (String pkg : p.packages) {
            if (installed.contains(pkg)) return pkg;
        }
        return null;
    }

    private static Set<String> installedPackages(Context context) {
        Set<String> out = new HashSet<String>();
        PackageManager pm = context.getPackageManager();
        for (Provider p : PROVIDERS) {
            for (String pkg : p.packages) {
                try {
                    pm.getApplicationInfo(pkg, 0);
                    out.add(pkg);
                } catch (Throwable ignored) {
                }
            }
        }
        return out;
    }

    /** Changes whenever an installed provider is added or removed. */
    public static String signature(Context context) {
        Set<String> installed = installedPackages(context);
        StringBuilder sb = new StringBuilder();
        for (Provider p : PROVIDERS) {
            String pkg = resolve(p, installed);
            if (pkg != null) sb.append(p.key).append(':').append(pkg).append(';');
        }
        return sb.toString();
    }

    public static int installedProviderCount(Context context) {
        Set<String> installed = installedPackages(context);
        int count = 0;
        for (Provider p : PROVIDERS) if (resolve(p, installed) != null) count++;
        return count;
    }

    public static String providerSummary(Context context) {
        String names = providerNames(context);
        if (names.startsWith("No known")) {
            return names + "\n\nRecommendations rotate every five minutes and open the "
                    + "provider that supplies each title.";
        }
        return "Installed: " + names + "\n\nRecommendations rotate every five minutes "
                + "and open the provider that supplies each title.";
    }

    /** Short, single-line list for Settings rows. */
    public static String providerNames(Context context) {
        Set<String> installed = installedPackages(context);
        StringBuilder sb = new StringBuilder();
        for (Provider p : PROVIDERS) {
            if (resolve(p, installed) == null) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(p.name);
        }
        if (sb.length() == 0) {
            return "No known OTT apps detected";
        }
        return sb.toString();
    }

    /** Reads the persisted rotation, advancing it when five minutes have elapsed. */
    public static int rotationForNow(Context context) {
        int rotation = Prefs.getInt(context, Prefs.K_REC_ROTATION, 0);
        long last = Prefs.getLong(context, Prefs.K_REC_LAST, 0);
        long now = System.currentTimeMillis();
        if (Prefs.getBool(context, Prefs.K_REC_AUTO, true)
                && (last <= 0 || now - last >= ROTATE_MS)) {
            rotation++;
            Prefs.putInt(context, Prefs.K_REC_ROTATION, rotation);
            Prefs.putLong(context, Prefs.K_REC_LAST, now);
        }
        return rotation;
    }

    /** Manual or timer-driven advance. */
    public static int advance(Context context) {
        int rotation = Prefs.getInt(context, Prefs.K_REC_ROTATION, 0) + 1;
        Prefs.putInt(context, Prefs.K_REC_ROTATION, rotation);
        Prefs.putLong(context, Prefs.K_REC_LAST, System.currentTimeMillis());
        return rotation;
    }

    public static void markRotated(Context context) {
        Prefs.putLong(context, Prefs.K_REC_LAST, System.currentTimeMillis());
    }
}
