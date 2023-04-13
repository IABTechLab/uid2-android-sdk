package com.uid2.dev

import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory

class MainActivity : AppCompatActivity() {

    private val LOGTAG = "IMABasicSample"
    private val SAMPLE_VIDEO_URL = "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"

    // Factory class for creating SDK objects.
    private val sdkFactory: ImaSdkFactory? = null

    // The AdsLoader instance exposes the requestAds method.
    private val adsLoader: AdsLoader? = null

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private val adsManager: AdsManager? = null

    // The saved content position, used to resumed content following an ad break.
    private val savedPosition = 0

    // This sample uses a VideoView for content and ad playback. For production
    // apps, Android's Exoplayer offers a more fully featured player compared to
    // the VideoView.
    private val videoPlayer: VideoView? = null
    private val mediaController: MediaController? = null
    private val playButton: View? = null
    private val videoAdPlayerAdapter: VideoAdPlayerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}
