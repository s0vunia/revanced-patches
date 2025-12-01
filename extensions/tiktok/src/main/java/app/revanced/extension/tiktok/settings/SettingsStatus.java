package app.revanced.extension.tiktok.settings;

public class SettingsStatus {
    public static boolean feedFilterEnabled = false;
    public static boolean downloadEnabled = false;
    public static boolean simSpoofEnabled = false;
    public static boolean telegramRedirectEnabled = false;

    public static void enableFeedFilter() {
        feedFilterEnabled = true;
    }

    public static void enableDownload() {
        downloadEnabled = true;
    }

    public static void enableSimSpoof() {
        simSpoofEnabled = true;
    }

    public static void enableTelegramRedirect() {
        telegramRedirectEnabled = true;
    }

    public static void load() {

    }
}
