package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdSimulationState(
    val isShowing: Boolean = false,
    val isRewarded: Boolean = false,
    val title: String = "",
    val description: String = "",
    val rewardLabel: String = "",
    val onRewarded: () -> Unit = {},
    val onDismissed: () -> Unit = {}
)

class AdMobManager(private val context: Context) {

    companion object {
        private const val TAG = "AdMobManager"
        // User AdMob Ad Unit IDs
        const val BANNER_ID = "ca-app-pub-5782293160806349/6965177176"
        const val INTERSTITIAL_ID = "ca-app-pub-5782293160806349/6602215096"
        const val REWARDED_ID = "ca-app-pub-5782293160806349/1465438484"

        // Aliases for compatibility
        const val TEST_BANNER_ID = BANNER_ID
        const val TEST_INTERSTITIAL_ID = INTERSTITIAL_ID
        const val TEST_REWARDED_ID = REWARDED_ID

        private const val MIN_INTERSTITIAL_INTERVAL_MS = 45_000L // 45 seconds
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialTime = 0L
    private var isAdMobInitialized = false

    // In-app visual overlay simulation state (for preview/offline/test scenarios)
    private val _simulationState = MutableStateFlow(AdSimulationState())
    val simulationState: StateFlow<AdSimulationState> = _simulationState.asStateFlow()

    init {
        try {
            val reqConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(reqConfig)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialized: $status")
                isAdMobInitialized = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadInterstitialAd() {
        try {
            val request = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                INTERSTITIAL_ID,
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        Log.d(TAG, "InterstitialAd loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        Log.w(TAG, "InterstitialAd load note: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error requesting InterstitialAd", e)
        }
    }

    fun loadRewardedAd() {
        try {
            val request = AdRequest.Builder().build()
            RewardedAd.load(
                context,
                REWARDED_ID,
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        Log.d(TAG, "RewardedAd loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        Log.w(TAG, "RewardedAd load note: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error requesting RewardedAd", e)
        }
    }

    fun showInterstitial(activity: Activity, onFinished: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastInterstitialTime < MIN_INTERSTITIAL_INTERVAL_MS) {
            onFinished()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            try {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        lastInterstitialTime = System.currentTimeMillis()
                        interstitialAd = null
                        loadInterstitialAd()
                        onFinished()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        interstitialAd = null
                        lastInterstitialTime = System.currentTimeMillis()
                        loadInterstitialAd()
                        onFinished()
                    }
                }
                ad.show(activity)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Exception showing InterstitialAd, falling back to overlay", e)
            }
        }

        // Trigger in-app overlay presentation
        lastInterstitialTime = System.currentTimeMillis()
        _simulationState.value = AdSimulationState(
            isShowing = true,
            isRewarded = false,
            title = "إعلان أدموب بيني (AdMob Interstitial)",
            description = "مساحة إعلان تجريبية بين المراحل والفصول",
            rewardLabel = "متابعة اللعب",
            onDismissed = {
                dismissSimulation()
                onFinished()
            }
        )
        loadInterstitialAd()
    }

    fun showRewarded(
        activity: Activity,
        rewardLabel: String,
        onRewarded: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            try {
                var rewardEarned = false
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        loadRewardedAd()
                        if (rewardEarned) {
                            onRewarded()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        rewardedAd = null
                        loadRewardedAd()
                        showSimulatedRewarded(rewardLabel, onRewarded)
                    }
                }
                ad.show(activity) { rewardItem ->
                    Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    rewardEarned = true
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "Exception showing RewardedAd, falling back to overlay", e)
            }
        }

        showSimulatedRewarded(rewardLabel, onRewarded)
        loadRewardedAd()
    }

    private fun showSimulatedRewarded(rewardLabel: String, onRewarded: () -> Unit) {
        _simulationState.value = AdSimulationState(
            isShowing = true,
            isRewarded = true,
            title = "إعلان أدموب بمكافأة (AdMob Rewarded)",
            description = "مساحة إعلان AdMob بمكافأة تجريبية",
            rewardLabel = rewardLabel,
            onRewarded = {
                dismissSimulation()
                onRewarded()
            },
            onDismissed = {
                dismissSimulation()
            }
        )
    }

    fun dismissSimulation() {
        _simulationState.value = _simulationState.value.copy(isShowing = false)
    }
}
