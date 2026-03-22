package org.hiten.blogspherebackend.util;

import org.hiten.blogspherebackend.repository.PostRepository;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    public static String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("^-|-$", ""); // Remove leading/trailing hyphens

        return slug;
    }

    public static String generateUniqueSlug(String title, PostRepository repository) {
        String baseSlug = generateSlug(title);

        if (baseSlug.isEmpty()) {
            baseSlug = "post";
        }

        String slug = baseSlug;
        int counter = 1;

        while (repository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }


    public static String generateUniqueSlug(String title, String currentSlug, PostRepository repository) {
        String baseSlug = generateSlug(title);

        if (baseSlug.isEmpty()) {
            baseSlug = "post";
        }

        // If the generated slug is the same as current, keep it
        if (baseSlug.equals(currentSlug)) {
            return currentSlug;
        }

        String slug = baseSlug;
        int counter = 1;

        while (repository.existsBySlug(slug) && !slug.equals(currentSlug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}

