package app.revanced.patches.tiktok.watermark

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for ByteWatermark constructor.
 * Target: com.ss.bytertc.engine.video.ByteWatermark
 * Constructor takes 4 float parameters: x, y, width, height
 */
internal val byteWatermarkFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    parameters("F", "F", "F", "F")
    custom { method, classDef ->
        classDef.type.endsWith("ByteWatermark;") ||
        classDef.type.contains("Watermark") && method.name == "<init>"
    }
}

/**
 * Fingerprint for the video download address getter.
 * Used to get the no-watermark download URL.
 * Target: Aweme model class - getDownloadAddr method
 */
internal val videoDownloadAddrFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Lcom/ss/android/ugc/aweme/feed/model/VideoUrlStruct;")
    custom { method, classDef ->
        method.name == "getDownloadAddr" &&
        (classDef.type.contains("Video") || classDef.type.contains("Aweme"))
    }
}

/**
 * Fingerprint for H265 play address getter (often watermark-free).
 * Target: Video model - getPlayAddrH265 method
 */
internal val videoPlayAddrH265Fingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Lcom/ss/android/ugc/aweme/feed/model/VideoUrlStruct;")
    custom { method, classDef ->
        (method.name == "getPlayAddrH265" || method.name == "getPlayAddrBytevc1") &&
        classDef.type.contains("Video")
    }
}

/**
 * Fingerprint for watermark visibility setter.
 * Target: UI layer watermark view - setVisibility
 */
internal val watermarkViewFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters("I")
    strings("watermark", "tiktok_watermark")
    custom { method, classDef ->
        method.name == "setVisibility" &&
        (classDef.type.contains("Watermark") || classDef.type.contains("Logo"))
    }
}

/**
 * Fingerprint for the image watermark application.
 * Target: Image processing - addWatermark method
 */
internal val imageWatermarkFingerprint = fingerprint {
    returns("Landroid/graphics/Bitmap;")
    strings("watermark", "addWatermark", "logo")
    custom { method, _ ->
        method.name.contains("watermark", ignoreCase = true) ||
        method.name.contains("addLogo", ignoreCase = true)
    }
}

/**
 * Fingerprint for getting video URI without watermark.
 * Target: Video model - getUri or getVideoUri method
 */
internal val videoUriFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    custom { method, classDef ->
        (method.name == "getUri" || method.name == "getVideoUri" || method.name == "getPlayUrl") &&
        classDef.type.contains("Video")
    }
}

/**
 * Fingerprint for UrlModel that contains download URLs.
 * Target: com.ss.android.ugc.aweme.feed.model.UrlModel
 */
internal val urlModelFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/util/List;")
    custom { method, classDef ->
        method.name == "getUrlList" &&
        classDef.type.contains("UrlModel")
    }
}
