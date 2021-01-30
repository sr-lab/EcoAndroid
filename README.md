# EcoAndroid

<p align="center">
  <img src="logo.png" alt="EcoAndroid logo" />
</p>

EcoAndroid is an [Android Studio](https://developer.android.com/studio) plugin that suggests automated refactorings for reducing energy consumption of Java android applications. It is also compatible with IntelliJ.

## Installation

### For users of the plugin
For now, you have to install the plugin manually by following these steps:

1. Clone this repository
2. Run `gradle buildPlugin`.
3. The plugin file is available in the folder `build/distributions/`. You can now install it.

In a near future, we plan to upload the plugin to the JetBrains Plugins Repository so that users can install it directly from Android Studio.

### For developers
If you are interested in changing or exploring the source code of the plugin, follow these steps:

1. Clone this repository
2. Open the project in your favourite IDE
3. Once you made the desired changes, run `gradle runIde`. An IntelliJ IDE with the plugin activated will open.

If you wish to add a new refactoring to the plugin, follow these steps:

1. Add a \<localInspection\> element to the plugin.xml file present in `src/main/resources`, following the specifications in the other inspections
2. Create a subclass of *com.intellij.codeInspection.LocalInspectionTool* (the one stated in the previous step) in the folder of the corresponding energy pattern
3. Create a subclass of *com.intellij.codeInspection.LocalQuickFix* to apply the refactoring to the source code

## EcoAndroid in Practice
EcoAndroid has been used to improve the energy efficiency of several Android applications. Examples include:

 - [Timetable](https://gitlab.com/asdoi/TimeTable): Cache Energy Pattern([#3](https://gitlab.com/asdoi/TimeTable/-/merge_requests/3))
 - [SecondScreen](https://github.com/farmerbb/SecondScreen): Dynamic Retry Delay and Cache Energy Patterns ([#72](https://github.com/farmerbb/SecondScreen/pull/72))
 - [ZimLX](https://github.com/otakuhqz/ZimLX): Cache Energy Pattern ([#85](https://github.com/otakuhqz/ZimLX/pull/85))
 - [Twire](https://github.com/twireapp/Twire): Cache Energy Pattern ([#111](https://github.com/twireapp/Twire/pull/111))
 - [Taskbar](https://github.com/farmerbb/Taskbar): Cache Energy Pattern ([#138](https://github.com/farmerbb/Taskbar/pull/138))
 - [Audinaut](https://github.com/nvllsvm/Audinaut): Cache Energy Pattern ([#93](https://github.com/nvllsvm/Audinaut/pull/93))
 - [Inwallet](https://github.com/btcontract/lnwallet): Cache Energy Pattern ([#26](https://github.com/btcontract/lnwallet/pull/26))
 - [DownloadNavi](https://github.com/TachibanaGeneralLaboratories/download-navi): Cache Energy Pattern ([#104](https://github.com/TachibanaGeneralLaboratories/download-navi/pull/104))
 - [Omega](https://github.com/otakuhqz/Omega): Cache Energy Pattern ([#3](https://github.com/otakuhqz/Omega/pull/3))
 - [Onpc](https://github.com/De7vID/klingon-assistant-android): Reduce Size Energy Pattern ([#90](https://github.com/De7vID/klingon-assistant-android/pull/90))
 - [tracker-control-android](https://github.com/OxfordHCC/tracker-control-android): Reduce Size Energy Pattern ([#130](https://github.com/OxfordHCC/tracker-control-android/pull/130))
 - [Vanilla-music-lyrics-search](https://github.com/vanilla-music/vanilla-music-lyrics-search): Reduce Size Energy Pattern ([#14](https://github.com/vanilla-music/vanilla-music-lyrics-search/pull/14))
 - [DokuwikiAndroid](https://github.com/fabienli/DokuwikiAndroid): Reduce Size Energy Pattern ([#13](https://github.com/fabienli/DokuwikiAndroid/pull/13))

Have you used EcoAndroid to improve your application? Please let us know so that we can add it to the list above!

## Energy Patterns
The energy patterns that are currently detected by EcoAndroid are:

  1. *Dynamic Retry Delay*, with the following cases:
      - Switching the time a thread goes to sleep from constant to dynamic (only considering the cases where variables are used to put the thread to sleep)
      - Giving information to the developer about a package to aid while dealing with the retry of work
      - Checking the network connection before processing a thread

  2. *Push over Poll*, with the following case:
      - Giving information to the developer about the possibility of implementing push notifications through Firebase Cloud Messaging (FCM) instead of a polling service

  3. *Reduce Size*, with the following case:
      - Asking for the response to be received compressed by the gzip scheme, if possible

  4.  *Cache*, with the following cases:
      - Checking the metadata before reloading information
      - Verifying the size of a view before resetting said view
      - Switching to *LocationManager.PASSIVE_PROVIDER* when invoking the method *requestLocationUpdates* from the class *android.location.LocationManager*
      - Increasing the size of the cache on a SSL Session
      - Creating a *TODO* in the source code for when nothing is different since the last update from an URL connection

  5. *Avoid Graphics and Animations*, with the following case:
      - Switching the rendering mode to *GLSurfaceView.RENDERMODE_WHEN_DIRTY*

The examples in the folder src/test are retrieved from real-life mobile applications.
In order to make the files more readable, parts of the original source code are either removed or commented.

## Resources
For more information about energy patterns, we recommend Cruz and Abreu's [open catalogue of energy-related patterns in mobile applications](https://tqrg.github.io/energy-patterns/#/).