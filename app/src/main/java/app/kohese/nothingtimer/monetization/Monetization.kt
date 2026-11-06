package app.kohese.nothingtimer.monetization

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

const val REWARD_MAX_PER_MONTH = 7

/** Everything the UI needs to know about Nodoro Pro / ads. */
data class ProStatus(
    /** Pro is currently active (purchase, rewarded pass, or the all-unlocked GitHub build). */
    val isPro: Boolean = false,
    /** Pro was bought (not just a rewarded 24h pass). */
    val ownsPro: Boolean = false,
    val passRemainingMs: Long = 0L,
    val passGrantsLeft: Int = REWARD_MAX_PER_MONTH,
    val adReady: Boolean = false,
    val price: String? = null,
    val busy: Boolean = false,
    val pending: Boolean = false,
    val message: String? = null
)

/**
 * Monetization boundary. The `play` flavor implements it with Google Play Billing + AdMob;
 * the `github` flavor unlocks everything and contains no ads, billing or network code.
 */
interface Monetization {
    /** True when everything is unlocked for free and there is nothing to buy (GitHub build). */
    val isFullVersion: Boolean
    val status: StateFlow<ProStatus>

    /** Call once from the activity: consent flow + ads SDK start. */
    fun init(activity: Activity)

    /** Re-check purchases and the rewarded pass (resume, restore). */
    fun refresh()

    fun purchase(activity: Activity)
    fun watchRewardedAd(activity: Activity)
    fun clearMessage()

    /** A focus session finished; may schedule an interstitial for the next natural break. */
    fun onFocusSessionCompleted()

    /** Shows a due interstitial, then calls [onDone]. Calls [onDone] immediately when nothing is shown. */
    fun showInterstitialIfDue(activity: Activity, onDone: () -> Unit)

    fun release()
}



