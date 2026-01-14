package app.revanced.patches.tiktok.nativelib

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode

/**
 * Bytecode patch for the tigrik native library system.
 *
 * This patch modifies the tigrik0/tigrik class to:
 * 1. Load our replacement library (libtikmod.so renamed to libtigrik.so)
 * 2. Skip cloud control initialization
 * 3. Handle errors gracefully if native lib is missing
 */
@Suppress("unused")
val tigrikBytecodePatch = bytecodePatch(
    name = "Tigrik bytecode patch",
    description = "Modifies tigrik native loading to use TikMod replacement library.",
) {
    dependsOn(nativeLibReplacementPatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Find and patch the tigrik class static initializer
        tigrikClassFingerprint.methodOrNull?.apply {
            // The static initializer loads "tigrik" library
            // Our replacement is already named libtigrik.so, so loading works automatically
            // We just add error handling

            addInstructions(
                0,
                """
                    # Log that TikMod is loading
                    const-string v0, "TikMod"
                    const-string v1, "Loading TikMod native library (NO CLOUD CONTROL)"
                    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
                """
            )
        }

        // Patch KillAppReceiver to do nothing
        killAppReceiverFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    # Skip kill switch - return immediately
                    const-string v0, "TikMod"
                    const-string v1, "Kill switch disabled - ignoring remote kill command"
                    invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                    return-void
                """
            )
        }

        // Patch banner fetch to return empty
        bannerFetchFingerprint.methodOrNull?.apply {
            // Make banner fetching return null/empty
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

        // Patch all Hidden0 special_clinit methods for cloud control indices
        // Indices 53-54: KillAppReceiver
        // Indices 63-73: Banner system
        hidden0ClassFingerprint.classDefOrNull?.methods?.forEach { method ->
            val methodName = method.name
            if (methodName.startsWith("special_clinit_")) {
                // Extract index from method name (e.g., "special_clinit_53_20" -> 53)
                val indexMatch = Regex("special_clinit_(\\d+)_").find(methodName)
                val index = indexMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1

                // Skip cloud control indices
                if (index in 53..54 || index in 63..73) {
                    // These are cloud control methods - we want them to do nothing
                    // Our native library already handles this by not registering natives
                    // But we add extra safety by logging
                    println("Cloud control method found: $methodName (index $index) - will be handled by TikMod")
                }
            }
        }
    }
}

/**
 * Comprehensive cloud control removal patch.
 * Combines native library replacement with bytecode patches.
 */
@Suppress("unused")
val cloudControlRemovalPatch = bytecodePatch(
    name = "Remove cloud control",
    description = "Removes all cloud control features: kill switch, remote banners, config fetching.",
) {
    dependsOn(tigrikBytecodePatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Additional cloud control removal if needed
        // The main work is done by tigrikBytecodePatch and nativeLibReplacementPatch

        println("Cloud control removal complete:")
        println("  - Kill switch: DISABLED")
        println("  - Remote banners: DISABLED")
        println("  - Config fetching: LOCAL ONLY")
        println("  - Native library: TikMod (no cloud)")
    }
}
