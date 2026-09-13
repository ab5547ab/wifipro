package com.example.whatswifiswitch

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Tries to figure out which WhatsApp conversation is currently open, by
 * looking for the contact/group name shown in the toolbar, and compares it
 * against the target the user configured (a contact name or phone number).
 *
 * This is inherently a best-effort heuristic: WhatsApp doesn't publish
 * stable resource IDs, and a WhatsApp update can change the layout enough
 * to break this. If it stops working after an update, the fix is to find
 * the current resource id of the toolbar title (e.g. with an accessibility
 * node inspector) and add it to KNOWN_TITLE_IDS below.
 */
object ChatTargetMatcher {

    private val KNOWN_TITLE_IDS = listOf(
        "com.whatsapp:id/conversation_contact_name",
        "com.whatsapp:id/title",
        "com.whatsapp:id/name"
    )

    /**
     * Returns true if the currently open WhatsApp screen appears to be a
     * conversation with the configured target contact/number.
     */
    fun isTargetChatOpen(root: AccessibilityNodeInfo?, target: String): Boolean {
        if (root == null || target.isBlank()) return false

        val titleText = findTitleText(root) ?: return false
        return matches(titleText, target)
    }

    private fun findTitleText(root: AccessibilityNodeInfo): String? {
        // 1. Try known resource IDs first (most reliable when they match).
        for (id in KNOWN_TITLE_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            val text = nodes?.firstOrNull()?.text?.toString()
            if (!text.isNullOrBlank()) return text
        }

        // 2. Fallback heuristic: look for a short text node near the top of
        // the screen (typical toolbar position), skipping obviously-wrong
        // matches like long message bubbles.
        return findTopBarTextFallback(root)
    }

    private fun findTopBarTextFallback(root: AccessibilityNodeInfo): String? {
        val bounds = android.graphics.Rect()
        root.getBoundsInScreen(bounds)
        val screenHeight = bounds.height().takeIf { it > 0 } ?: return null
        val topBandLimit = bounds.top + (screenHeight * 0.12).toInt()

        var best: String? = null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()
            if (!text.isNullOrBlank() && text.length in 1..40) {
                val nodeBounds = android.graphics.Rect()
                node.getBoundsInScreen(nodeBounds)
                if (nodeBounds.top in bounds.top..topBandLimit) {
                    best = text
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return best
    }

    private fun matches(titleText: String, target: String): Boolean {
        val normalizedTitle = titleText.trim().lowercase()
        val normalizedTarget = target.trim().lowercase()

        // Name-style match.
        if (normalizedTitle.contains(normalizedTarget)) return true

        // Phone-number-style match: compare digits only.
        val titleDigits = titleText.filter { it.isDigit() }
        val targetDigits = target.filter { it.isDigit() }
        if (targetDigits.length >= 7 && titleDigits.endsWith(targetDigits)) return true

        return false
    }
}
