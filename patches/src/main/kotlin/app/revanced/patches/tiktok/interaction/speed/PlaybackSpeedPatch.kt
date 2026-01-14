package app.revanced.patches.tiktok.interaction.speed

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.shared.getEnterFromFingerprint
import app.revanced.patches.tiktok.shared.onRenderFirstFrameFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Patch to enable and remember playback speed settings.
 * Note: This patch requires specific fingerprints to match. If they don't match
 * (e.g., TikTok 43.x with different class structure), the patch will skip gracefully.
 */
@Suppress("unused")
val playbackSpeedPatch = bytecodePatch(
    name = "Playback speed",
    description = "Enables the playback speed option for all videos and " +
        "retains the speed configurations in between videos.",
) {
    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Check if all required fingerprints match
        val setSpeedMethod = setSpeedFingerprint.methodOrNull ?: return@execute
        val getSpeedMethod = getSpeedFingerprint.methodOrNull ?: return@execute
        val renderFrameMethod = onRenderFirstFrameFingerprint.methodOrNull ?: return@execute
        val enterFromMethod = getEnterFromFingerprint.originalMethodOrNull ?: return@execute

        getSpeedMethod.apply {
            val injectIndex = implementation?.instructions?.indexOfFirst {
                it.getReference<MethodReference>()?.returnType == "F"
            }?.takeIf { it >= 0 }?.plus(2) ?: return@execute

            val register = getInstruction<Instruction11x>(injectIndex - 1).registerA

            addInstruction(
                injectIndex,
                "invoke-static { v$register }," +
                    " Lapp/revanced/extension/tiktok/speed/PlaybackSpeedPatch;->rememberPlaybackSpeed(F)V",
            )
        }

        // By default, the playback speed will reset to 1.0 at the start of each video.
        // Instead, override it with the desired playback speed.
        // Note: Use move-object/from16 to handle parameter registers that may exceed v15
        renderFrameMethod.addInstructions(
            0,
            """
                # Copy p0 to local register since it may be > v15
                move-object/from16 v3, p0

                # Video playback location (e.g. home page, following page or search result page) retrieved using getEnterFrom method.
                const/4 v0, 0x1
                invoke-virtual { v3, v0 },  $enterFromMethod
                move-result-object v0

                # Model of current video retrieved using getCurrentAweme method.
                invoke-virtual { v3 }, Lcom/ss/android/ugc/aweme/feed/panel/BaseListFragmentPanel;->getCurrentAweme()Lcom/ss/android/ugc/aweme/feed/model/Aweme;
                move-result-object v1

                # Desired playback speed retrieved using getPlaybackSpeed method.
                invoke-static { }, Lapp/revanced/extension/tiktok/speed/PlaybackSpeedPatch;->getPlaybackSpeed()F
                move-result v2
                invoke-static { v0, v1, v2 }, ${setSpeedFingerprint.originalMethod}
            """,
        )

        // Force enable the playback speed option for all videos.
        setSpeedFingerprint.classDef.methods.find { method -> method.returnType == "Z" }?.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
