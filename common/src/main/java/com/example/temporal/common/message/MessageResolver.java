package com.example.temporal.common.message;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class for resolving internationalized messages.
 * Supports message formatting with placeholders.
 */
@Slf4j
public final class MessageResolver {

    private static final String BUNDLE_NAME = "messages.messages";
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private MessageResolver() {
        // Utility class - prevent instantiation
    }

    /**
     * Resolves an error message using the default locale.
     *
     * @param messageKey the message key
     * @return the resolved message, or the key if not found
     */
    public static String resolveError(final String messageKey) {
        return resolve(messageKey, DEFAULT_LOCALE, null);
    }

    /**
     * Resolves an error message with arguments using the default locale.
     *
     * @param messageKey the message key
     * @param args       the message arguments
     * @return the resolved message, or the key if not found
     */
    public static String resolveError(final String messageKey, final Object... args) {
        return resolve(messageKey, DEFAULT_LOCALE, args);
    }

    /**
     * Resolves an error message using the specified locale.
     *
     * @param messageKey the message key
     * @param locale     the locale
     * @return the resolved message, or the key if not found
     */
    public static String resolveError(final String messageKey, final Locale locale) {
        return resolve(messageKey, locale, null);
    }

    /**
     * Resolves an error message with arguments using the specified locale.
     *
     * @param messageKey the message key
     * @param locale     the locale
     * @param args       the message arguments
     * @return the resolved message, or the key if not found
     */
    public static String resolveError(
            final String messageKey,
            final Locale locale,
            final Object... args) {
        return resolve(messageKey, locale, args);
    }

    /**
     * Resolves a success message using the default locale.
     *
     * @param messageKey the message key
     * @return the resolved message, or the key if not found
     */
    public static String resolveSuccess(final String messageKey) {
        return resolve(messageKey, DEFAULT_LOCALE, null);
    }

    /**
     * Resolves a success message with arguments using the default locale.
     *
     * @param messageKey the message key
     * @param args       the message arguments
     * @return the resolved message, or the key if not found
     */
    public static String resolveSuccess(final String messageKey, final Object... args) {
        return resolve(messageKey, DEFAULT_LOCALE, args);
    }

    /**
     * Resolves a success message with arguments using the specified locale.
     *
     * @param messageKey the message key
     * @param locale     the locale
     * @param args       the message arguments
     * @return the resolved message, or the key if not found
     */
    public static String resolveSuccess(
            final String messageKey,
            final Locale locale,
            final Object... args) {
        return resolve(messageKey, locale, args);
    }

    /**
     * Resolves a message with the specified locale and arguments.
     *
     * @param messageKey the message key
     * @param locale     the locale
     * @param args       the message arguments
     * @return the resolved message, or the key if not found
     */
    private static String resolve(
            final String messageKey,
            final Locale locale,
            final Object... args) {
        try {
            final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            final String pattern = bundle.getString(messageKey);

            if (args != null && args.length > 0) {
                return MessageFormat.format(pattern, args);
            }

            return pattern;
        } catch (Exception e) {
            log.warn("Message not found for key: {} in locale: {}", messageKey, locale, e);
            return messageKey;
        }
    }

    /**
     * Gets the default locale used for message resolution.
     *
     * @return the default locale
     */
    public static Locale getDefaultLocale() {
        return DEFAULT_LOCALE;
    }
}
