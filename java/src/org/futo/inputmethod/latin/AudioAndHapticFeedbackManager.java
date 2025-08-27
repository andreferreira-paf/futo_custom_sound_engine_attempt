/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.latin;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

import org.futo.inputmethod.latin.common.Constants;
import org.futo.inputmethod.latin.settings.SettingsValues;

// New imports for custom sound engine
import android.media.SoundPool;
import android.media.AudioAttributes;
import org.futo.inputmethod.latin.R;
import java.util.HashMap;
import android.content.res.Resources;
import android.content.Context;
import android.util.Log;

/**
 * This class gathers audio feedback and haptic feedback functions.
 *
 * It offers a consistent and simple interface that allows LatinIME to forget about the
 * complexity of settings and the like.
 */
public final class AudioAndHapticFeedbackManager {
    private AudioManager mAudioManager;
    private Vibrator mVibrator;

    private SettingsValues mSettingsValues;
    private boolean mSoundOn;
    // New variables for custom sound engine
    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> mSoundMap = new HashMap<>();
    private boolean mSoundsLoaded = false;
    private String mCustomKeypressSoundsProfile; // User preference for custom sounds
    private Context mContext;
    private static final java.lang.String TAG = "AudioFeedbackManager";

    private static final AudioAndHapticFeedbackManager sInstance =
            new AudioAndHapticFeedbackManager();

    public static AudioAndHapticFeedbackManager getInstance() {
        return sInstance;
    }

    private AudioAndHapticFeedbackManager() {
        // Intentional empty constructor for singleton.
    }

    public static void init(final Context context) {
        sInstance.initInternal(context);
    }

    private void initInternal(final Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        
        // Initialize SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION) // Good for UI sounds
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(4) // Max simultaneous sounds
                .setAudioAttributes(audioAttributes)
                .build();

        mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                Log.d(TAG, "Sound loaded successfully: " + sampleId);
            } else {
                Log.e(TAG, "Error loading sound " + sampleId + ", status: " + status);
            }
        });
    try {
        loadSoundsForCurrentProfile(context);
        mSoundsLoaded = true; // Assume loading starts; use OnLoadCompleteListener for better accuracy
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e(TAG, "Error loading sounds: A resource was not found. Check your res/raw folder and R class. " + e.getMessage());
            mSoundsLoaded = false;
        }
    }

    private void loadSoundsForCurrentProfile(final Context context) {
        for (int soundId : mSoundMap.values()) {
            mSoundPool.unload(soundId);
        }
        mSoundMap.clear();
        mSoundsLoaded = false; // Reset until new sounds are loaded

        Log.d(TAG, "Loading sounds for profile: " + mCustomKeypressSoundsProfile);

        try {
            Resources res = context.getResources();
            int deleteSoundResId;
            int enterSoundResId;
            int spaceSoundResId;
            int keypressSoundResId;
            int keypressSoundResIds[];

            switch (mSettingsValues.mCustomKeypressSoundsProfile) {
                case 0: // blue profile
                    Log.i(TAG, "Using 'blue' sound profile.");
                    deleteSoundResId = R.raw.blue_delete;
                    enterSoundResId = R.raw.blue_enter;
                    spaceSoundResId = R.raw.blue_space;
                    keypressSoundResIds = res.getIntArray(R.array.blue_keypress_sound_res_ids);
                break;
                case 1: // red profile
                    Log.i(TAG, "Using 'default' sound profile.");
                    deleteSoundResId = R.raw.default_delete; // Fallback to your 'enter_bruh' or specific defaults
                    enterSoundResId = R.raw.default_enter;
                    spaceSoundResId = R.raw.default_space;
                    keypressSoundResId = R.raw.default_keypress;
                break;
                default: // Fallback to old method, loading nothing
                    //load nothing!
                    mSoundsLoaded = false;
                    return;
                break;
            }

            // Load Special sounds into SoundPool and map them in mSoundMap (a hashmap)
            int deleteSoundId = mSoundPool.load(context, deleteSoundResId, 1);
            int enterSoundId = mSoundPool.load(context, enterSoundResId, 1);
            int spaceSoundId = mSoundPool.load(context, spaceSoundResId, 1);
            mSoundMap.put(Constants.CODE_DELETE, deleteSoundId);
            mSoundMap.put(Constants.CODE_ENTER, enterSoundId);
            mSoundMap.put(Constants.CODE_SPACE, spaceSoundId);

            //Load all other keypress sounds and map them in a for loop (there's too many to do manually)
            for (int i = 0; i < keypressSoundResIds.length; i++) {
                mSoundPool.load(context, keypressSoundResIds[i], 1);
            }
            int subtractor = 33; // ASCII code for space + 1, i.e. space is excluded
            int numberOfUniqueSounds = keypressSoundResIds.length;
            int indexOfFinalSound = keypressSoundResIds.length - 1;
            for (int asciiCode=33; asciiCode<=255; asciiCode++) {
                mSoundMap.put(asciiCode, keypressSoundResIds[asciiCode - subtractor]);
                if (asciiCode - subtractor == indexOfFinalSound) {
                    subtractor += numberOfUniqueSounds;
                }
            }
            mSoundsLoaded = true;

        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e(TAG, "Error loading sounds: A resource was not found for profile " + mCustomKeypressSoundsProfile + ". Check your res/raw folder and R class. " + e.getMessage());
            mSoundsLoaded = false;
        } catch (java.lang.Exception e) {
            Log.e(TAG, "Unexpected error loading sounds for profile " + mCustomKeypressSoundsProfile + ": " + e.getMessage());
            mSoundsLoaded = false;
        }
    }
    public void performHapticAndAudioFeedback(final int code,
            final View viewToPerformHapticFeedbackOn) {
        performHapticFeedback(viewToPerformHapticFeedbackOn, false);
        performAudioFeedback(code);
    }

    public boolean hasVibrator() {
        return mVibrator != null && mVibrator.hasVibrator();
    }

    public void vibrate(final long milliseconds) {
        if (mVibrator == null) {
            return;
        }
        mVibrator.vibrate(milliseconds);
    }

    private boolean reevaluateIfSoundIsOn() {
        if (mSettingsValues == null || !mSettingsValues.mSoundOn || mAudioManager == null) {
            return false;
        }
        return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    public void performAudioFeedback(final int code) {
        Log.d(TAG, "performAudioFeedback - code: " + code + ", mSoundOn: " + mSettingsValues.mSoundOn);
        // if mAudioManager is null, we can't play a sound anyway, so return
        if (mAudioManager == null) {
            return;
        }
        if (!mSoundOn) {
            return;
        }
        if (mSoundsLoaded) {
            int soundId = mSoundMap.get(code);
            mSoundPool.play(soundId, mSettingsValues.mKeypressSoundVolume, mSettingsValues.mKeypressSoundVolume, 1, 0, 1.0f);
            return;
        } else {
            Log.w(TAG, "Custom soundId not found or failed to load for code: " + code);
        }
        if (mAudioManager == null){
            return;
        }
        final int sound;
        switch (code) {
        case Constants.CODE_DELETE:
            sound = AudioManager.FX_KEYPRESS_DELETE;
            break;
        case Constants.CODE_ENTER:
            sound = AudioManager.FX_KEYPRESS_RETURN;
            break;
        case Constants.CODE_SPACE:
            sound = AudioManager.FX_KEYPRESS_SPACEBAR;
            break;
        default:
            sound = AudioManager.FX_KEYPRESS_STANDARD;
            break;
        }
        mAudioManager.playSoundEffect(sound, mSettingsValues.mKeypressSoundVolume);
    }

    public void performHapticFeedback(final View viewToPerformHapticFeedbackOn, final boolean repeatKey) {
        if (!mSettingsValues.mVibrateOn) {
            return;
        }
        if (mSettingsValues.mKeypressVibrationDuration >= 0) {
            vibrate(mSettingsValues.mKeypressVibrationDuration / (repeatKey ? 2 : 1));
            return;
        }
        // Go ahead with the system default
        if (viewToPerformHapticFeedbackOn != null) {
            viewToPerformHapticFeedbackOn.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    public void onSettingsChanged(final SettingsValues settingsValues) {
        mSettingsValues = settingsValues;
        loadSoundsForCurrentProfile(mContext);
        mSoundOn = reevaluateIfSoundIsOn();
    }

    public void onRingerModeChanged() {
        mSoundOn = reevaluateIfSoundIsOn();
    }

    public void release() {
        Log.d(TAG, "Releasing SoundPool.");
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
        mSoundMap.clear();
        mSoundsLoaded = false;
    }
}
