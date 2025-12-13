package app.revanced.patches.tiktok.sslpinning

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/sslpinning/SslPinningBypass;"

/**
 * Patch to disable SSL certificate pinning in TikTok.
 *
 * This patch allows HTTPS traffic inspection using proxy tools like:
 * - Burp Suite
 * - mitmproxy
 * - Charles Proxy
 *
 * The patch hooks multiple certificate validation points:
 * 1. OkHttp CertificatePinner.check() - main pinning mechanism
 * 2. X509TrustManager.checkServerTrusted() - certificate trust validation
 * 3. HostnameVerifier.verify() - hostname validation
 * 4. SignatureVerificationInterceptor - ByteDance's request signing
 *
 * WARNING: This patch should only be used for debugging purposes.
 * It makes the app vulnerable to MITM attacks when enabled.
 *
 * The bypass is controlled by a setting and is OFF by default.
 */
@Suppress("unused")
val sslPinningPatch = bytecodePatch(
    name = "Disable SSL pinning",
    description = "Disables certificate pinning to allow HTTPS traffic inspection with proxy tools.",
) {
    dependsOn(sharedExtensionPatch, settingsPatch)

    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
        "com.zhiliaoapp.musically"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
    )

    execute {
        // Hook 1: Bypass OkHttp CertificatePinner.check()
        // This is the primary certificate pinning mechanism
        certificatePinnerCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBypassPinning()Z
                    move-result v0
                    if-eqz v0, :continue
                    return-void
                    :continue
                    nop
                """
            )
        }

        // Hook 1b: Also try obfuscated fingerprint
        certificatePinnerCheckObfuscatedFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBypassPinning()Z
                    move-result v0
                    if-eqz v0, :continue
                    return-void
                    :continue
                    nop
                """
            )
        }

        // Hook 2: Bypass CertificatePinner.Builder.add() to prevent pin registration
        certificatePinnerBuilderAddFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBypassPinning()Z
                    move-result v0
                    if-eqz v0, :continue
                    return-object p0
                    :continue
                    nop
                """
            )
        }

        // Hook 3: Bypass X509TrustManager.checkServerTrusted()
        trustManagerCheckServerTrustedFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBypassPinning()Z
                    move-result v0
                    if-eqz v0, :continue
                    return-void
                    :continue
                    nop
                """
            )
        }

        // Hook 4: Make HostnameVerifier always return true
        hostnameVerifierFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBypassPinning()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x1
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 5: Disable SignatureVerificationInterceptor
        // This is ByteDance's request signing mechanism
        signatureVerificationInterceptorFingerprint.methodOrNull?.apply {
            // Find the config.enable check and make it return false
            // The interceptor checks LLILII.enable field early and returns if false
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldDisableSignatureVerification()Z
                    move-result v0
                    if-eqz v0, :continue
                    # If bypass enabled, just proceed with the chain without verification
                    check-cast p1, LX/0Z1Y;
                    iget-object v1, p1, LX/0Z1Y;->LIZJ:Lcom/bytedance/retrofit2/client/Request;
                    invoke-virtual {p1, v1}, LX/0Z1Y;->LIZ(Lcom/bytedance/retrofit2/client/Request;)LX/0YZU;
                    move-result-object v0
                    return-object v0
                    :continue
                    nop
                """
            )
        }

        // Hook 6: Patch SignatureCertConfig to disable by default
        signatureCertConfigFingerprint.methodOrNull?.apply {
            // After constructor, set enable = false
            val instructionCount = implementation!!.instructions.size
            addInstruction(
                instructionCount - 1,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldDisableSignatureVerification()Z
                    move-result v0
                    if-eqz v0, :skip_disable
                    const/4 v0, 0x0
                    iput-boolean v0, p0, Lcom/bytedance/pipo/security/ab/SignatureCertConfig;->enable:Z
                    :skip_disable
                """
            )
        }

        // Hook 7: Patch OkHttpClient.Builder.certificatePinner() to return null pinner
        okHttpClientBuilderFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBypassPinning()Z
                    move-result v0
                    if-eqz v0, :continue
                    # Return this without setting the pinner
                    return-object p0
                    :continue
                    nop
                """
            )
        }

        // Log that the patch was applied
        // This helps verify the patch is active during debugging
    }
}
