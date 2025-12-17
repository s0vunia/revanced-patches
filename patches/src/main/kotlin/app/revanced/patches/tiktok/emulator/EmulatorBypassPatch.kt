package app.revanced.patches.tiktok.emulator

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsComposePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/emulator/EmulatorBypass;"

/**
 * Patch to bypass emulator detection in TikTok.
 *
 * TikTok refuses to run on emulators for various reasons (fraud prevention, etc.)
 * This patch spoofs device properties to make TikTok think it's running on a real device.
 *
 * Bypasses:
 * 1. Build.* property checks (HARDWARE, MODEL, FINGERPRINT, etc.)
 * 2. File existence checks (/dev/qemu_pipe, /dev/goldfish_pipe, etc.)
 * 3. System property checks (ro.hardware, ro.kernel.qemu, etc.)
 * 4. TelephonyManager checks (IMEI, phone number)
 * 5. Sensor availability checks
 * 6. ByteDance security SDK checks
 * 7. CPU architecture checks (x86 detection)
 *
 * The bypass is controlled by a setting and is OFF by default.
 */
@Suppress("unused")
val emulatorBypassPatch = bytecodePatch(
    name = "Bypass emulator detection",
    description = "Allows TikTok to run on emulators by spoofing device properties.",
) {
    dependsOn(sharedExtensionPatch, settingsComposePatch)

    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
        "com.zhiliaoapp.musically"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
    )

    execute {
        // Hook 1: Bypass generic emulator detection methods
        // These typically return true if emulator is detected
        emulatorDetectionFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x0
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 2: Bypass device check methods
        deviceCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x0
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 3: Bypass file existence checks for emulator-specific files
        fileExistsCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x0
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 4: Bypass ByteDance security SDK checks
        bytedanceSecurityCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x0
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 5: Bypass CPU architecture checks
        // x86/x86_64 often indicates emulator
        cpuArchCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    # Return arm64-v8a to appear as real device
                    const-string v0, "arm64-v8a"
                    return-object v0
                    :continue
                    nop
                """
            )
        }

        // Hook 6: Bypass AppsFlyer emulator check if present
        appsFlyerEmulatorCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x0
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 7: Bypass root detection (often coupled with emulator detection)
        rootDetectionFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x0
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 8: Spoof sensor availability
        sensorCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    const/4 v0, 0x1
                    return v0
                    :continue
                    nop
                """
            )
        }

        // Hook 9: Bypass battery check (emulators always show as charging)
        batteryCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isBypassEnabled()Z
                    move-result v0
                    if-eqz v0, :continue
                    # Pretend battery is discharging (not plugged in)
                    const/4 v0, 0x0
                    return v0
                    :continue
                    nop
                """
            )
        }
    }
}
