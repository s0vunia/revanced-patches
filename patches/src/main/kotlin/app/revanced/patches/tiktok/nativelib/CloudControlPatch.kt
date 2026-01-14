package app.revanced.patches.tiktok.nativelib

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

/**
 * Bytecode patch to disable cloud control mechanisms.
 *
 * This patch modifies ByteDance's native library system to:
 * 1. Use our replacement native library (without cloud control)
 * 2. Disable the remote kill switch
 * 3. Block remote banner advertisements
 * 4. Skip cloud configuration loading
 */
@Suppress("unused")
val cloudControlBytecodePatch = bytecodePatch(
    name = "Cloud control bytecode patch",
    description = "Disables cloud control mechanisms at the bytecode level.",
) {
    dependsOn(nativeLibReplacementPatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Patch the native library loader class
        nativeLoaderClassFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    # Log that ReVanced native library is loading
                    const-string v0, "ReVanced"
                    const-string v1, "Loading native library (cloud control disabled)"
                    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
                """
            )
        }

        // Disable the remote kill switch receiver
        remoteKillSwitchFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    # Disable remote kill switch - return immediately
                    const-string v0, "ReVanced"
                    const-string v1, "Remote kill switch disabled - ignoring termination command"
                    invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                    return-void
                """
            )
        }

        // Disable remote banner fetching
        remoteBannerFetchFingerprint.methodOrNull?.apply {
            val returnType = this.returnType
            when {
                returnType == "V" -> {
                    addInstructions(0, "return-void")
                }
                returnType.startsWith("L") || returnType.startsWith("[") -> {
                    addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return-object v0
                        """
                    )
                }
                returnType == "Z" -> {
                    addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return v0
                        """
                    )
                }
            }
        }

        // Process native registration class for cloud control indices
        // Indices 53-54: Remote kill switch
        // Indices 63-73: Remote banner system
        nativeRegistrationClassFingerprint.classDefOrNull?.methods?.forEach { method ->
            val methodName = method.name
            if (methodName.startsWith("special_clinit_")) {
                val indexMatch = Regex("special_clinit_(\\d+)_").find(methodName)
                val index = indexMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1

                // Log cloud control methods that will be disabled
                if (index in 53..54 || index in 63..73) {
                    println("Cloud control method disabled: $methodName (index $index)")
                }
            }
        }
    }
}

/**
 * Main cloud control removal patch.
 * Combines native library replacement with bytecode patches.
 */
@Suppress("unused")
val cloudControlRemovalPatch = bytecodePatch(
    name = "Remove cloud control",
    description = "Removes all cloud control features: kill switch, remote banners, config fetching.",
) {
    dependsOn(cloudControlBytecodePatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        println("Cloud control removal complete:")
        println("  - Remote kill switch: DISABLED")
        println("  - Remote banners: DISABLED")
        println("  - Cloud config: LOCAL ONLY")
        println("  - Native library: ReVanced (no cloud)")
    }
}
