package com.ecommerce.platform.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for generating URL-friendly slugs from strings.
 */
public final class SlugUtil {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");

    private SlugUtil() {
        // Utility class - no instantiation
    }

    /**
     * Convert a string to a URL-friendly slug.
     * Examples:
     *   "Hello World" -> "hello-world"
     *   "Men's Clothing" -> "mens-clothing"
     *   "Café & Restaurant" -> "cafe-restaurant"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);

        // Remove leading/trailing dashes
        slug = slug.replaceAll("^-+|-+$", "");

        return slug;
    }
}
