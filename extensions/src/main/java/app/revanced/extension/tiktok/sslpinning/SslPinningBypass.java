package app.revanced.extension.tiktok.sslpinning;

import android.util.Log;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import app.revanced.extension.tiktok.settings.Settings;

/**
 * SSL Pinning Bypass for TikTok.
 * Provides utilities to disable certificate validation for debugging/traffic inspection.
 *
 * WARNING: This should only be used for debugging purposes.
 * Enabling this allows MITM attacks - use with caution!
 */
public final class SslPinningBypass {

    private static final String TAG = "SslPinningBypass";

    private static volatile X509TrustManager trustAllManager;
    private static volatile HostnameVerifier trustAllHostnameVerifier;
    private static volatile SSLSocketFactory trustAllSocketFactory;

    private SslPinningBypass() {
        // Static utility class
    }

    /**
     * Check if SSL pinning bypass is enabled in settings.
     */
    public static boolean isBypassEnabled() {
        return Settings.isSslBypassEnabled();
    }

    /**
     * Get a TrustManager that accepts all certificates.
     * Caches the instance for reuse.
     */
    public static X509TrustManager getTrustAllManager() {
        if (trustAllManager == null) {
            synchronized (SslPinningBypass.class) {
                if (trustAllManager == null) {
                    trustAllManager = new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            // Accept all client certificates
                            if (isBypassEnabled()) {
                                Log.d(TAG, "Bypassing client certificate check");
                            }
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // Accept all server certificates
                            if (isBypassEnabled()) {
                                Log.d(TAG, "Bypassing server certificate check for: " +
                                    (chain != null && chain.length > 0 ? chain[0].getSubjectDN() : "unknown"));
                            }
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    };
                }
            }
        }
        return trustAllManager;
    }

    /**
     * Get a HostnameVerifier that accepts all hostnames.
     */
    public static HostnameVerifier getTrustAllHostnameVerifier() {
        if (trustAllHostnameVerifier == null) {
            synchronized (SslPinningBypass.class) {
                if (trustAllHostnameVerifier == null) {
                    trustAllHostnameVerifier = new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            if (isBypassEnabled()) {
                                Log.d(TAG, "Bypassing hostname verification for: " + hostname);
                                return true;
                            }
                            return false;
                        }
                    };
                }
            }
        }
        return trustAllHostnameVerifier;
    }

    /**
     * Get an SSLSocketFactory that trusts all certificates.
     */
    public static SSLSocketFactory getTrustAllSocketFactory() {
        if (trustAllSocketFactory == null) {
            synchronized (SslPinningBypass.class) {
                if (trustAllSocketFactory == null) {
                    try {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, new TrustManager[]{getTrustAllManager()}, new SecureRandom());
                        trustAllSocketFactory = sslContext.getSocketFactory();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to create TrustAll SSLSocketFactory", e);
                    }
                }
            }
        }
        return trustAllSocketFactory;
    }

    /**
     * Get TrustManager array for use with SSLContext.init().
     * Returns trust-all managers if bypass is enabled, null otherwise.
     */
    public static TrustManager[] getTrustManagers() {
        if (isBypassEnabled()) {
            return new TrustManager[]{getTrustAllManager()};
        }
        return null;
    }

    /**
     * Hook for CertificatePinner.check() - makes it a no-op when bypass is enabled.
     * Called from bytecode patch.
     */
    public static boolean shouldBypassPinning() {
        boolean bypass = isBypassEnabled();
        if (bypass) {
            Log.d(TAG, "Bypassing certificate pinning check");
        }
        return bypass;
    }

    /**
     * Hook for signature verification interceptor.
     * Returns true if signature verification should be disabled.
     */
    public static boolean shouldDisableSignatureVerification() {
        boolean bypass = isBypassEnabled();
        if (bypass) {
            Log.d(TAG, "Disabling signature verification");
        }
        return bypass;
    }

    /**
     * Log when SSL bypass is active (for debugging).
     */
    public static void logBypassActive(String location) {
        if (isBypassEnabled()) {
            Log.i(TAG, "SSL bypass active at: " + location);
        }
    }
}
