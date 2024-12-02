package com.example.youtubeskip

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 1. monitoring foreground package change
 */
class YoutubeAccessibilitySerice : AccessibilityService() {
    val rect = Rect()
    var job: Job? = null

    override fun onInterrupt() {
    }

    fun log(message: String) {
        Log.i("AccessibilityEvent", message)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        log("onEvent: $event")

        if (event.contentChangeTypes == CONTENT_CHANGE_TYPE_PANE_DISAPPEARED) {
            // todo stop checking. but, appear comes at the same time
            log("Youtube disappeared")
        }

        job?.cancel()
        job = GlobalScope.launch {
            while (true) {
                delay(2000)
                val nodeInfo = getRootView()
                if (nodeInfo == null) {
                    job?.cancel()
                    job = null
                }
                log("node: $nodeInfo")
                getYoutubeSkip(nodeInfo)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }

        }
    }

    fun getRootView(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        }
    }

    private fun getYoutubeSkip(nodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (nodeInfo == null) {
            return null
        }

        if (nodeInfo.isYoutubeSkip()) {
            return nodeInfo
        }

        if (nodeInfo.childCount > 0) {
            for (i in 0 until nodeInfo.childCount) {
                getYoutubeSkip(nodeInfo.getChild(i))?.let {
                    return it
                }
            }
        }

        return null
    }

    fun getClickableNodes(nodeInfo: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        nodeInfo ?: return emptyList()

        val result = mutableListOf<AccessibilityNodeInfo>()
        if (nodeInfo.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK}) {
            result.add(nodeInfo)
        }

        if (nodeInfo.childCount > 0) {
            for (i in 0 until nodeInfo.childCount) {
                result.addAll(getClickableNodes(nodeInfo.getChild(i)))
            }
        }

        return result
    }


    fun AccessibilityNodeInfo.isYoutubeSkip(): Boolean {
        if (contentDescription != null || text != null) {
            return false
        }

        if (className != "android.widget.FrameLayout") {
            return false
        }

        if (!actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK}) {
            return false
        }

        getBoundsInScreen(rect)

        val width = rect.width()
        val height = rect.height()

        if (width > getScreenWidth() / 2) {
            return false
        }

        if (height > getScreenHeight() / 3) {
            return false
        }

        val widthHeightRatio = width.toFloat() / height
        log("ratio : $widthHeightRatio, rect: $rect")

        val rectVertical = Rect(getScreenWidth() - dp2px(113), getScreenHeight()/10, getScreenWidth(), getScreenHeight())//there is vertical ad as well
        val rectHorizontal = Rect(getScreenHeight() - dp2px(113), getScreenWidth() - dp2px(48) - dp2px(36), getScreenHeight(), getScreenWidth() - dp2px(36))
        return rect.intersect(rectVertical) || rect.intersect(rectHorizontal)
    }

    fun getScreenWidth(): Int =
        if (resources.displayMetrics.widthPixels < resources.displayMetrics.heightPixels) resources.displayMetrics.widthPixels
        else resources.displayMetrics.heightPixels

    fun getScreenHeight(): Int =// in Pixels
        if (resources.displayMetrics.widthPixels > resources.displayMetrics.heightPixels) resources.displayMetrics.widthPixels
        else resources.displayMetrics.heightPixels

    fun dp2px(dp: Int): Int {
        return dp * resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT    // px
    }
}
