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
import android.content.res.TypedArray;
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
import java.lang.Integer;
import org.futo.inputmethod.latin.settings.Settings;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
    private int mLastSelectedProfile;
    private boolean mSoundsLoaded = false;
    private Context mContext;
    private int mExpectedSoundCount = 0;
    private int mLoadedSoundCount = 0;
    private static final String TAG = "AudioFeedbackManager";
    private int deleteSoundId;
    private int enterSoundId;
    private int spaceSoundId;
    private int[] keypressSoundId;
    private int numberOfUniqueSounds;
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
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(4) // Max simultaneous sounds
                .setAudioAttributes(audioAttributes)
                .build();

        mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) { // success
                mLoadedSoundCount++;
                Log.d(TAG, "Sound loaded: " + sampleId +
                        " (" + mLoadedSoundCount + "/" + mExpectedSoundCount + ")");
                if (mLoadedSoundCount >= mExpectedSoundCount) {
                    mapAllSounds();
                    Log.i(TAG, "All sounds loaded successfully");
                }
            } else {
                Log.e(TAG, "Error loading sound " + sampleId + ", status: " + status);
            }
        });
    }
    private void mapAllSounds() {
        mSoundMap.put(Constants.CODE_DELETE, deleteSoundId);
        mSoundMap.put(Constants.CODE_ENTER, enterSoundId);
        mSoundMap.put(Constants.CODE_SPACE, spaceSoundId);

        for (int asciiCode = 33; asciiCode <= 255; asciiCode++) {
            int soundIndex = (asciiCode - 33) % numberOfUniqueSounds; // cycles automatically
            Log.i(TAG, "Mapping code: " + asciiCode + " to " + keypressSoundId[soundIndex]);
            mSoundMap.put(asciiCode, keypressSoundId[soundIndex]);
        }
        mSoundsLoaded = true;
    }

    private void loadSoundsForCurrentProfile() {
        if (mSoundPool != null) {
            for (int soundId : mSoundMap.values()) {
                mSoundPool.unload(soundId);
            }
        }
        mSoundMap.clear();
        mSoundsLoaded = false;
        mExpectedSoundCount = 0;
        mLoadedSoundCount = 0;
        Log.d(TAG, "Loading sounds for profile: " + mSettingsValues.mCustomKeypressSoundsProfile);

        try {
            Resources res = mContext.getResources();
            int deleteSoundResId = 0;
            int enterSoundResId = 0;
            int spaceSoundResId = 0;
            int[] keypressSoundResIds;
            TypedArray ta;
            switch (mSettingsValues.mCustomKeypressSoundsProfile) {
                case Settings.BLUE_KEYPRESS_PROFILE: // blue profile
                    Log.i(TAG, "Using 'blue' sound profile.");
                    deleteSoundResId = R.raw.blue_delete;
                    enterSoundResId = R.raw.blue_enter;
                    spaceSoundResId = R.raw.blue_space;
                    ta = res.obtainTypedArray(R.array.blue_keypress_sound_res_ids);
                    keypressSoundResIds = new int[ta.length()];
                    for (int i = 0; i < ta.length(); i++) {
                        keypressSoundResIds[i] = ta.getResourceId(i, 0);
                    }
                    ta.recycle();
                    mLastSelectedProfile = Settings.BLUE_KEYPRESS_PROFILE;
                    break;
                case Settings.RED_KEYPRESS_PROFILE: // red profile
                    Log.i(TAG, "Using 'default' sound profile.");
                    deleteSoundResId = R.raw.default_delete;
                    enterSoundResId = R.raw.default_enter;
                    spaceSoundResId = R.raw.default_space;
                    keypressSoundResIds = new int[] { R.raw.default_keypress };
                    mLastSelectedProfile = Settings.RED_KEYPRESS_PROFILE;
                break;
                default: // Fallback to old method, loading nothing
                    Log.i(TAG, "FALLBACK TO DEFAULT METHOD");
                    keypressSoundResIds = new int[] {0};
                    mLastSelectedProfile = Settings.DEFAULT_KEYPRESS_PROFILE;
                break;
            }
            if (keypressSoundResIds[0] == 0){
                Log.i(TAG, "Exiting because keypresSoundResIds = 0");
                return;
            }
            mExpectedSoundCount = keypressSoundResIds.length + 3;
            //Loading Sounds

            deleteSoundId = mSoundPool.load(mContext, deleteSoundResId, 1);
            enterSoundId = mSoundPool.load(mContext, enterSoundResId, 1);
            spaceSoundId = mSoundPool.load(mContext, spaceSoundResId, 1);
            numberOfUniqueSounds = keypressSoundResIds.length;

            keypressSoundId = new int[numberOfUniqueSounds];
            int currentIndex = 0;
            for (Integer soundResId : keypressSoundResIds) {
                Log.i(TAG, "Loading sound: " + soundResId);
                keypressSoundId[currentIndex] = mSoundPool.load(mContext, soundResId, 1);
                currentIndex += 1;
            }

        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Error loading sounds: A resource was not found for profile " + mSettingsValues.mCustomKeypressSoundsProfile + ". Check your res/raw folder and R class. " + e.getMessage());
            mSoundsLoaded = false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error loading sounds for profile " + mSettingsValues.mCustomKeypressSoundsProfile + ": " + e.getMessage());
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
        if (!mSoundOn) {
            return;
        }
        if (mSoundsLoaded) {
            int soundId = mSoundMap.getOrDefault(code, 0);
            if (soundId != 0){
                mSoundPool.play(soundId, mSettingsValues.mKeypressSoundVolume, mSettingsValues.mKeypressSoundVolume, 1, 0, 1.0f);
                return;
            } else {
                Log.e(TAG, "Sound Id is 0 but should not be!");
            }

        } else {
            Log.w(TAG, "Custom soundId not found or failed to load for code: " + code);
        }
        // if mAudioManager is null, we can't play a sound anyway, so return
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
        mSoundOn = reevaluateIfSoundIsOn();
        if (mSettingsValues.mCustomKeypressSoundsProfile != mLastSelectedProfile) {
            loadSoundsForCurrentProfile();
        }
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
