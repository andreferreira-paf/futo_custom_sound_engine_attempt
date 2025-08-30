# FUTO Keyboard (with custom sound)

This is a fork of the futo keyboard that adds the ability to use custom sounds for keyboard output. But it doesn't let the user add custom sounds. A developer needs to come in and add the sounds themselves.

Having users add their OWN custom sounds falls outside the scope of this project (at least for now), though that would be so effing cool, bro. For real for real. 

Feel free to take have a look and test it out for yourself. The sounds are simple right now and not varied, but it's still fun to play with.


### Known Issue(s):
- Only latin characters are supported for now, i.e. Cyrillic, Japanese, Korean, etc. are not supported.
- Red Switches profile still not working and has BRUH meme sound as placeholder lol.


### Building

When cloning the repository, you must perform a recursive clone to fetch all dependencies:
```
git clone --recursive https://github.com/andreferreira-paf/futo_custom_sound_engine_attempt.git
```

If you forgot to specify recursive clone, use this to fetch submodules:
```
git submodule update --init --recursive
```

You can then open the project in Android Studio and build it that way, or use gradle commands:
```
./gradlew assembleUnstableDebug
./gradlew assembleStableRelease
```