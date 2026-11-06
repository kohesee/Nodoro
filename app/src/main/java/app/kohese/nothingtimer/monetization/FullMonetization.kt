package app.kohese.nothingtimer.monetization

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** GitHub / open build: everything unlocked, no ads, no billing, no network. */
fun createMonetization(app: Application, scope: CoroutineScope): Monetization = FullMonetization

private object FullMonetization : Monetization {
    override val isFullVersion = true
    override val status: StateFlow<ProStatus> = MutableStateFlow(ProStatus(isPro = true, ownsPro = true))

    override fun init(activity: Activity) = Unit
    override fun refresh() = Unit
    override fun purchase(activity: Activity) = Unit
    override fun watchRewardedAd(activity: Activity) = Unit
    override fun clearMessage() = Unit
    override fun onFocusSessionCompleted() = Unit
    override fun showInterstitialIfDue(activity: Activity, onDone: () -> Unit) = onDone()
    override fun release() = Unit
}

