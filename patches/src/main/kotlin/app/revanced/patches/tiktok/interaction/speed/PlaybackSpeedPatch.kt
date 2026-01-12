package app.revanced.patches.tiktok.interaction.speed

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.shared.getEnterFromFingerprint
import app.revanced.patches.tiktok.shared.onRenderFirstFrameFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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
        setSpeedFingerprint.let { onVideoSwiped ->
            getSpeedFingerprint.method.apply {
                val injectIndex =
                    indexOfFirstInstructionOrThrow { getReference<MethodReference>()?.returnType == "F" } + 2
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
            onRenderFirstFrameFingerprint.method.addInstructions(
                0,
                """
                    # Copy p0 to local register since it may be > v15
                    move-object/from16 v3, p0

                    # Video playback location (e.g. home page, following page or search result page) retrieved using getEnterFrom method.
                    const/4 v0, 0x1
                    invoke-virtual { v3, v0 },  ${getEnterFromFingerprint.originalMethod}
                    move-result-object v0

                    # Model of current video retrieved using getCurrentAweme method.
                    invoke-virtual { v3 }, Lcom/ss/android/ugc/aweme/feed/panel/BaseListFragmentPanel;->getCurrentAweme()Lcom/ss/android/ugc/aweme/feed/model/Aweme;
                    move-result-object v1

                    # Desired playback speed retrieved using getPlaybackSpeed method.
                    invoke-static { }, Lapp/revanced/extension/tiktok/speed/PlaybackSpeedPatch;->getPlaybackSpeed()F
                    move-result v2
                    invoke-static { v0, v1, v2 }, ${onVideoSwiped.originalMethod}
                """,
            )

            // Force enable the playback speed option for all videos.
            onVideoSwiped.classDef.methods.find { method -> method.returnType == "Z" }?.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
        }
    }
}
