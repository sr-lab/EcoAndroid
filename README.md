# EcoAndroid

<p align="center">
  <img src="logo.png" alt="EcoAndroid logo" />
</p>

EcoAndroid is an [Android Studio](https://developer.android.com/studio) plugin that suggests automated refactorings for reducing energy consumption of Java android applications.

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

## EcoAndroid in Practice
EcoAndroid has been used to improve the energy efficiency of several Android applications. Examples include:

 - [SecondScreen](https://github.com/farmerbb/SecondScreen): Dynamic Retry Delay and Cache Energy Patterns ([#72](https://github.com/farmerbb/SecondScreen/pull/72))
 - [tracker-control-android](https://github.com/OxfordHCC/tracker-control-android): Reduce Size Energy Pattern ([#130](https://github.com/OxfordHCC/tracker-control-android/pull/130))
 - [Taskbar](https://github.com/farmerbb/Taskbar): Cache Energy Pattern ([#138](https://github.com/farmerbb/Taskbar/pull/138))

Have you used EcoAndroid to improve your application? Please let us know so that we can add it to the list above!

## Energy Patterns
The energy patterns that are currently detected by EcoAndroid are:

  1. *Dynamic Retry Delay*, with the following cases:
      - Switching the time a thread goes to sleep from constant to dynamic (only considering the cases where variables are used to put the thread to sleep)
      - Giving information to the developer about a package to aid while dealing with the retry of work
      - Checking the network connection before processing a thread

  2. *Push over Poll*, with the following case:
      - Giving information to the developer about the possibility of implementing push notifications through FCM instead of a polling service

  3.  *Cache*, with the following cases:
      - Checking the metadata before reloading information
      - Verifying the size of a view before resetting said view
      - Switching to *LocationManager.PASSIVE_PROVIDER* when invoking the method *requestLocationUpdates* from the class *android.location.LocationManager*
      - Increasing the size of the cache on a SSL Session
      - Creating a *TODO* in the source code for when nothing is different since the last update from an URL connection

  4. *Avoid Graphics and Animations*, with the following case:
      - Switching the rendering mode to *GLSurfaceView.RENDERMODE_WHEN_DIRTY*

  5. *Reduce Size*, with the following case:
      - Compressing with gzip before receiving data
 
In order to make the examples more readable and more compact, parts of the source code retrieved from the original applications have either been removed or commented. For every case in every energy pattern, there is an example under a directory with the same name. 