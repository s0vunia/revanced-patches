package app.revanced.patches.tiktok.interaction.cleardisplay

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for Clear Mode event handler.
 * - TikTok < 43.x: ClearModePanelComponent.onClearModeEvent()
 * - TikTok 43.x: PinchToClearModePanelComponent (different method structure)
 */
internal val onClearDisplayEventFingerprint = fingerprint {
    custom { method, classDef ->
        // TikTok < 43.x: ClearModePanelComponent
        // TikTok 43.x: PinchToClearModePanelComponent (renamed)
        (classDef.endsWith("/ClearModePanelComponent;") ||
            classDef.endsWith("/PinchToClearModePanelComponent;")) &&
            (method.name == "onClearModeEvent" || method.name.contains("ClearMode"))
    }
}
