package com.example.whatswifiswitch

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * Watches for window-state-changed events. When the foreground app switches
 * to WhatsApp we turn WiFi on and connect; when it switches away we turn
 * WiFi off.
 *
 * If a target contact/number is configured (see MainActivity), WiFi is only
 * kept on while that specific conversation is open on screen - switching to
 * another chat, the chat list, or another app turns it back off. If no
 * target is configured, WiFi stays on for as long as WhatsApp is open, as
 * before.
 */
class WhatsAppAccessibilityService : AccessibilityService() {

    private val whatsAppPackages = setOf("com.whatsapp", "com.whatsapp.w4b")
    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.example.whatswifiswitch"
    )

    private var whatsAppIsForeground = false
    private var targetChatCurrentlyOpen = false

    private val handler = Handler(Looper.getMainLooper())
    private var pendingCheck: Runnable? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName in ignoredPackages) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val isWhatsApp = packageName in whatsAppPackages
                if (isWhatsApp && !whatsAppIsForeground) {
                    whatsAppIsForeground = true
                    onWhatsAppOpened()
                } else if (!isWhatsApp && whatsAppIsForeground) {
                    whatsAppIsForeground = false
                    onWhatsAppClosed()
                }
                if (isWhatsApp) scheduleChatCheck()
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (whatsAppIsForeground && packageName in whatsAppPackages) {
                    scheduleChatCheck()
                }
            }
        }
    }

    /** Debounced check of which chat is open - content-changed events fire very often. */
    private fun scheduleChatCheck() {
        val target = getTargetContact() ?: return // no filter configured, nothing to check
        pendingCheck?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { checkOpenChat(target) }
        pendingCheck = runnable
        handler.postDelayed(runnable, 500)
    }

    private fun checkOpenChat(target: String) {
        val isTargetOpen = ChatTargetMatcher.isTargetChatOpen(rootInActiveWindow, target)
        if (isTargetOpen && !targetChatCurrentlyOpen) {
            targetChatCurrentlyOpen = true
            val networks = NetworkStore.getAll(applicationContext)
            if (networks.isNotEmpty()) {
                WifiController.turnOnAndConnect(applicationContext, networks)
            }
        } else if (!isTargetOpen && targetChatCurrentlyOpen) {
            targetChatCurrentlyOpen = false
            WifiController.turnOff(applicationContext)
        }
    }

    private fun onWhatsAppOpened() {
        val target = getTargetContact()
        if (target != null) {
            // Filter is configured: wait for the content check to confirm
            // the right chat is actually open before touching WiFi.
            return
        }
        val networks = NetworkStore.getAll(applicationContext)
        if (networks.isNotEmpty()) {
            WifiController.turnOnAndConnect(applicationContext, networks)
        }
    }

    private fun onWhatsAppClosed() {
        targetChatCurrentlyOpen = false
        WifiController.turnOff(applicationContext)
    }

    private fun getTargetContact(): String? {
        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val value = prefs.getString(Prefs.TARGET_CONTACT_KEY, null)
        return if (value.isNullOrBlank()) null else value
    }

    override fun onInterrupt() {
        // No-op
    }
}
