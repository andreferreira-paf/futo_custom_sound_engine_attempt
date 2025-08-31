# FUTO Keyboard (with custom sound)

This is a fork of the futo keyboard that adds the ability to use custom sounds for keyboard output. But it doesn't let the user add custom sounds. A developer needs to come in and add the sounds themselves.

Having users add their OWN custom sounds falls outside the scope of this project (at least for now), though that would be so effing cool, bro. For real for real. 


### Where Are the Sound Files From?
Most sounds are taken under the [MIT license](https://opensource.org/license/mit), from this project: [mechvibes](https://github.com/hainguyents13/mechvibes-dx).
Give them your support [here](https://mechvibes.com/support-me/).

#### Links for sound Sources
##### CherryMX (from [mechvibes](https://github.com/hainguyents13/mechvibes-dx))
- [CherryMX BLUE Switches](https://mechvibes.com/sound-packs/sound-pack-1200000000003/dist/cherrymx-blue-abs.zip) (by Mechvibes Team, edited by André Ferreira)
- [CherryMX RED Switches](https://mechvibes.com/sound-packs/sound-pack-1200000000007/dist/cherrymx-red-abs.zip) (not implemented yet)
- Other sound profiles will be slowly added over time from the [mechvibes](https://github.com/hainguyents13/mechvibes-dx) project. Please go support them [here](https://mechvibes.com/support-me/).


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