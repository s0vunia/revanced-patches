package app.revanced.patches.tiktok.download

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for the share/download menu button click handler.
 * This is triggered when user taps the share button on a video.
 */
internal val shareMenuClickFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    strings("share_panel", "download", "save_video")
    custom { method, classDef ->
        method.name.contains("onClick") ||
        method.name.contains("onShare") ||
        classDef.type.contains("SharePanel") ||
        classDef.type.contains("ShareMenu")
    }
}

/**
 * Fingerprint for the download button specifically.
 * TikTok has a dedicated download option in the share menu.
 */
internal val downloadButtonFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    strings("download_video", "save_local", "download")
    custom { method, classDef ->
        (method.name == "onClick" || method.name.contains("download") || method.name.contains("save")) &&
        (classDef.type.contains("Download") || classDef.type.contains("Save"))
    }
}

/**
 * Fingerprint for the actual download execution method.
 * This method handles the video download logic.
 */
internal val downloadExecuteFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters("Ljava/lang/String;") // Video URL
    strings("mp4", "video", "download", "aweme")
    custom { method, classDef ->
        method.name.contains("download") ||
        method.name.contains("save") ||
        classDef.type.contains("DownloadService") ||
        classDef.type.contains("VideoSaver")
    }
}

/**
 * Fingerprint for share panel item click.
 * Each item in the share panel (including download) triggers this.
 */
internal val sharePanelItemClickFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    custom { method, classDef ->
        method.name == "onItemClick" &&
        (classDef.type.contains("Share") || classDef.type.contains("Panel"))
    }
}

/**
 * Fingerprint for the download menu option initialization.
 * Used to identify where the download button is created.
 */
internal val downloadMenuInitFingerprint = fingerprint {
    returns("V")
    strings("download", "icon_download", "save_video", "save_to_device")
    custom { method, _ ->
        method.name.contains("init") ||
        method.name.contains("setup") ||
        method.name.contains("create")
    }
}

/**
 * Fingerprint for ACLCommonShare - controls what sharing options are available.
 */
internal val aclCommonShareFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("I")
    custom { method, classDef ->
        classDef.type.endsWith("/ACLCommonShare;") && method.name == "getCode"
    }
}

/**
 * Fingerprint for getting the video URL that would be downloaded.
 * We need this to potentially pass the video info to the Telegram bot.
 */
internal val getVideoUrlFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    custom { method, classDef ->
        (method.name == "getVideoUrl" || method.name == "getPlayUrl" || method.name == "getDownloadUrl") &&
        (classDef.type.contains("Video") || classDef.type.contains("Aweme"))
    }
}

/**
 * Fingerprint for getting aweme (video) ID.
 */
internal val getAwemeIdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    custom { method, classDef ->
        (method.name == "getAwemeId" || method.name == "getId" || method.name == "getVideoId") &&
        (classDef.type.contains("Aweme") || classDef.type.contains("Video"))
    }
}
