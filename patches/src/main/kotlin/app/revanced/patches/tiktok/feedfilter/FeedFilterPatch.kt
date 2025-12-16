package app.revanced.patches.tiktok.feedfilter

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
<<<<<<< HEAD
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsComposePatch
import app.revanced.patches.tiktok.misc.settings.settingsStatusLoadFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/tiktok/feedfilter/FeedItemsFilter;"

@Suppress("unused")
val feedFilterPatch = bytecodePatch(
    name = "Feed filter",
    description = "Removes ads, livestreams, stories, image videos " +
        "and videos with a specific amount of views or likes from the feed.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsComposePatch,
    )

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Hook FeedApiService.fetchFeedList() to filter main feed
        feedApiServiceLIZFingerprint.method.let { method ->
            val returnInstruction = method.instructions.first { it.opcode == Opcode.RETURN_OBJECT }
            val register = (returnInstruction as OneRegisterInstruction).registerA
            method.addInstruction(
                returnInstruction.location.index,
                "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->filter(Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;)V"
            )
        }

        // Hook FollowFeedList to filter following feed
        followFeedFingerprint.method.let { method ->
            val returnInstruction = method.instructions.first { it.opcode == Opcode.RETURN_OBJECT }
            val register = (returnInstruction as OneRegisterInstruction).registerA
            method.addInstruction(
                returnInstruction.location.index,
                "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->filter(Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;)V"
            )
        }

        settingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/revanced/extension/tiktok/settings/SettingsStatus;->enableFeedFilter()V",
        )
    }

=======
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/feedfilter/FeedItemsFilter;"

/**
 * Patch to remove ALL ads from TikTok feed.
 *
 * This patch works by:
 * 1. Intercepting the feed list before it's displayed
 * 2. Filtering out any items that are ads, promoted content, or sponsored
 * 3. Blocking the Pangle/Bytedance ad SDK initialization
 *
 * Supports both TikTok (com.zhiliaoapp.musically) and TikTok Lite (com.ss.android.ugc.trill)
 */
@Suppress("unused")
val feedFilterPatch = bytecodePatch(
    name = "Feed filter",
    description = "Removes ALL ads from TikTok feed including sponsored content, promoted videos, and live streams.",
) {
    dependsOn(sharedExtensionPatch, settingsPatch)

    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
        "com.zhiliaoapp.musically"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
    )

    execute {
        // Method 1: Filter feed items list
        feedItemListFingerprint.method.apply {
            // Find where the list is being set or returned
            val listInstructions = implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.INVOKE_VIRTUAL ||
                    instruction.opcode == Opcode.INVOKE_INTERFACE
                }
                .filter { (_, instruction) ->
                    instruction.toString().contains("List") ||
                    instruction.toString().contains("items")
                }

            // Insert filter call before the list is used
            listInstructions.lastOrNull()?.let { (index, _) ->
                addInstruction(
                    index,
                    "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->filterFeedItems(Ljava/lang/Object;)V"
                )
            }
        }

        // Method 2: Also hook the RecyclerView adapter
        feedRecyclerFingerprint.methodOrNull?.apply {
            // Get the register containing the list parameter
            val listRegister = 1 // Usually p1 for the list parameter

            addInstructions(
                0,
                """
                    invoke-static { p$listRegister }, $EXTENSION_CLASS_DESCRIPTOR->filterFeedList(Ljava/util/List;)Ljava/util/List;
                    move-result-object p$listRegister
                """
            )
        }

        // Method 3: Block Pangle Ad SDK initialization completely
        pangleAdInitFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBlockAds()Z
                    move-result v0
                    if-eqz v0, :continue
                    return-void
                    :continue
                    nop
                """
            )
        }

        // Method 4: Make isAd() always return false for non-ad items
        // and true for actual ads (so they get filtered)
        isAdCheckFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->isAdItem(Ljava/lang/Object;)Z
                    move-result v0
                    return v0
                """
            )
        }

        // Method 5: Intercept ad insertion
        adInsertFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBlockAds()Z
                    move-result v0
                    if-eqz v0, :allow
                    return-void
                    :allow
                    nop
                """
            )
        }
    }
>>>>>>> 2172b6f60 (feat: TikTok ReVanced standalone mod implementation)
}
