package com.w3engineers.mesh.util.log;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import com.w3engineers.mesh.util.MeshLog;

import java.util.Locale;

/**
 * Text to Speech provider
 */
public class MyTextSpeech implements TextToSpeech.OnInitListener {

    private boolean mHasInitiated;
    private TextToSpeech mSpeaker;


    public MyTextSpeech(Context context) {
        mSpeaker = new TextToSpeech(context, this);
    }


    public void stop() {
        if (mSpeaker != null) {
            mSpeaker.stop();
            mSpeaker.shutdown();
            mHasInitiated = false;
        }
    }

    @Override
    public void onInit(final int status) {
        if (status == TextToSpeech.SUCCESS) {
            mHasInitiated = true;
            if (mSpeaker != null){
                int result = mSpeaker.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    //
                }
                else {
//                String msg = "hi there, i'm ready";
//                speak(msg);
                }
            } else {
                MeshLog.e("mSpeaker not found");
            }

        }
        else {
           //_logger.error("Initialization Failed!");
        }
    }
    public void speak(final String text) {
        mSpeaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
