package com.aurora.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Hand-picked, self-contained movie suggestions. */
public final class MovieCatalog {

    public static final List<Movie> MOVIES;

    static {
        List<Movie> m = new ArrayList<Movie>();
        m.add(new Movie("dune2", "Dune: Part Two", "2024", "Sci-Fi · Epic", "2h 46m",
                "8.8", 98,
                "Paul Atreides unites with the Fremen and walks the path of revenge against the conspirators who destroyed his family.",
                "TRENDING", 0xFFC96B2A, 0xFF2B1208, 0xFFF4B86A));
        m.add(new Movie("br2049", "Blade Runner 2049", "2017", "Sci-Fi · Noir", "2h 44m",
                "8.0", 94,
                "A young blade runner uncovers a secret buried long enough to plunge what remains of society into chaos.",
                "NEO-NOIR", 0xFF2A7C8C, 0xFF07171F, 0xFF72D9E8));
        m.add(new Movie("interstellar", "Interstellar", "2014", "Sci-Fi · Drama", "2h 49m",
                "8.7", 96,
                "A team of explorers travels through a wormhole in space in an attempt to ensure humanity's survival.",
                "EPIC", 0xFF3D5FA8, 0xFF0A1024, 0xFFE9C47C));
        m.add(new Movie("thebatman", "The Batman", "2022", "Crime · Noir", "2h 56m",
                "7.8", 91,
                "When a sadistic serial killer begins murdering key political figures, Batman is forced to investigate the city's hidden corruption.",
                "DARK", 0xFFA33A3A, 0xFF160608, 0xFFF08A6A));
        m.add(new Movie("spiderverse", "Spider-Man: Across the Spider-Verse", "2023",
                "Animation · Action", "2h 20m", "8.6", 97,
                "Miles Morales is catapulted across the multiverse, where he encounters a team of Spider-People charged with protecting its existence.",
                "VIBRANT", 0xFF8B3FA8, 0xFF1C0726, 0xFFF06AA8));
        m.add(new Movie("oppenheimer", "Oppenheimer", "2023", "Drama · History", "3h 0m",
                "8.3", 93,
                "The story of J. Robert Oppenheimer and his role in the development of the atomic bomb.",
                "AWARD WINNER", 0xFFB07A3A, 0xFF120D08, 0xFFF2D08A));
        m.add(new Movie("arrival", "Arrival", "2016", "Sci-Fi · Mystery", "1h 56m",
                "7.9", 92,
                "A linguist is recruited by the military to communicate with alien lifeforms after twelve mysterious spacecraft land around the world.",
                "THOUGHTFUL", 0xFF6B7D8C, 0xFF0A1015, 0xFFD6E4EE));
        m.add(new Movie("sicario", "Sicario", "2015", "Crime · Thriller", "2h 1m",
                "7.6", 89,
                "An idealistic FBI agent is enlisted by a government task force to aid in the escalating war against drugs at the border.",
                "TENSE", 0xFF9A6B3A, 0xFF150E08, 0xFFE8C18A));
        MOVIES = Collections.unmodifiableList(m);
    }

    private MovieCatalog() {}

    public static Movie byId(String id) {
        for (Movie m : MOVIES) {
            if (m.id.equals(id)) return m;
        }
        return MOVIES.get(0);
    }
}
