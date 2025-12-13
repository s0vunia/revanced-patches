package app.revanced.patches.tiktok.sslpinning

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for OkHttp CertificatePinner.check() method.
 * This method throws SSLPeerUnverifiedException if certificate doesn't match pins.
 *
 * void check(String hostname, List<Certificate> peerCertificates)
 */
internal val certificatePinnerCheckFingerprint = fingerprint {
    returns("V")
    parameters("Ljava/lang/String;", "Ljava/util/List;")
    custom { method, classDef ->
        classDef.type.contains("CertificatePinner") &&
        method.name == "check"
    }
}

/**
 * Alternative fingerprint for obfuscated CertificatePinner.
 * TikTok may use obfuscated OkHttp classes.
 */
internal val certificatePinnerCheckObfuscatedFingerprint = fingerprint {
    returns("V")
    parameters("Ljava/lang/String;", "Ljava/util/List;")
    strings("Certificate pinning failure!", "Pinned certificates for")
}

/**
 * Fingerprint for CertificatePinner.Builder.add() method.
 * Prevent pins from being registered in the first place.
 *
 * Builder add(String pattern, String... pins)
 */
internal val certificatePinnerBuilderAddFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type.contains("CertificatePinner") &&
        classDef.type.contains("Builder") &&
        method.name == "add"
    }
}

/**
 * Fingerprint for X509TrustManager.checkServerTrusted() implementations.
 * TikTok may have custom TrustManager implementations.
 */
internal val trustManagerCheckServerTrustedFingerprint = fingerprint {
    returns("V")
    parameters("[Ljava/security/cert/X509Certificate;", "Ljava/lang/String;")
    custom { method, classDef ->
        method.name == "checkServerTrusted" &&
        classDef.interfaces.any { it.contains("X509TrustManager") }
    }
}

/**
 * Fingerprint for SSLContext.init() calls.
 * We can intercept this to inject our TrustManager.
 */
internal val sslContextInitFingerprint = fingerprint {
    returns("V")
    parameters("[Ljavax/net/ssl/KeyManager;", "[Ljavax/net/ssl/TrustManager;", "Ljava/security/SecureRandom;")
    custom { method, classDef ->
        method.name == "init" &&
        classDef.type == "Ljavax/net/ssl/SSLContext;"
    }
}

/**
 * Fingerprint for OkHttpClient.Builder to hook certificate pinner setup.
 */
internal val okHttpClientBuilderFingerprint = fingerprint {
    custom { method, classDef ->
        (classDef.type.contains("OkHttpClient") || classDef.type.contains("okhttp3")) &&
        classDef.type.contains("Builder") &&
        method.name == "certificatePinner"
    }
}

/**
 * Fingerprint for ByteDance SignatureCertConfig class.
 * Used to disable the signature verification interceptor.
 */
internal val signatureCertConfigFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type.contains("SignatureCertConfig") &&
        method.name == "<init>"
    }
}

/**
 * Fingerprint for SignatureVerificationInterceptor.intercept() method.
 * This is ByteDance's custom interceptor for request signing.
 */
internal val signatureVerificationInterceptorFingerprint = fingerprint {
    strings("x-pipo-signature", "x-pipo-certificate")
    custom { method, classDef ->
        classDef.type.contains("SignatureVerificationInterceptor") &&
        method.name == "intercept"
    }
}

/**
 * Fingerprint for HostnameVerifier implementations.
 */
internal val hostnameVerifierFingerprint = fingerprint {
    returns("Z")
    parameters("Ljava/lang/String;", "Ljavax/net/ssl/SSLSession;")
    custom { method, classDef ->
        method.name == "verify" &&
        classDef.interfaces.any { it.contains("HostnameVerifier") }
    }
}

/**
 * Fingerprint for network security config trust anchors.
 * Some apps define trusted certificates here.
 */
internal val networkSecurityTrustFingerprint = fingerprint {
    strings("network_security_config", "trust-anchors", "certificates")
}

/**
 * Fingerprint for ByteDance's TTNet/Cronet SSL configuration.
 * TikTok uses their own network stack.
 */
internal val ttnetSslConfigFingerprint = fingerprint {
    custom { method, classDef ->
        (classDef.type.contains("TTNet") || classDef.type.contains("Cronet")) &&
        method.name.lowercase().contains("ssl")
    }
}
