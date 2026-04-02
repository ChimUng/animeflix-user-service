package com.animeflix.aisearchservice.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class TextUtils {

    private TextUtils() {}

    /**
     * AniList trả về description có HTML tags (<br>, <i>, <b>...).
     * Strip HTML trước khi embed để vector chất lượng hơn.
     */
    public static String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        // Jsoup parse và strip tags, giữ lại text thuần
        String clean = Jsoup.clean(html, Safelist.none());
        // Decode HTML entities (&amp; → &, &lt; → <, ...)
        return org.jsoup.parser.Parser.unescapeEntities(clean, false).trim();
    }

    /**
     * Truncate text để tránh vượt token limit Gemini (1M tokens).
     * Thực tế: description anime hiếm khi dài quá 2000 chars.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}