# EcoAndroid
An Android Studio Plugin for Reducing Energy Consumption of Java Mobile Applications through Automated Refactoring.
<br/> 
The energy patterns the plugin can detect are the following: <br/>
  1. *Dynamic Retry Delay*, with the following cases:<br/>
   * Switching the time a thread goes to sleep from constant to dynamic (only considering the cases where variables are used to put the thread to sleep);<br/>
   * Giving information to the developer about a package to aid while dealing with the retry of work; <br/>
   * Checking the network connection before processing a thread. <br/>
  2. *Push over Poll*, with the following case: <br/>
   * Giving information to the developer about the possibility of implementing push notifications through FCM instead of a polling service. <br/>
  3.  *Cache*, with the following cases:<br/>
   * Checking the metadata before reloading information; <br/>
   * Verifying the size of a view before resetting said view. <br/>
   * Switching to *LocationManager.PASSIVE_PROVIDER* when invoking the method *requestLocationUpdates* from the class *android.location.LocationManager* <br/>
   * Increasing the size of the cache on a SSL Session <br/>
   * Creating a *TODO* in the source code for when nothing is different since the last update from an URL connection <br/>
  4. *Avoid Graphics and Animations*, with the following case:<br/>
   * Switching the rendering mode to *GLSurfaceView.RENDERMODE_WHEN_DIRTY* <br/>
  5. *Reduce Size*, with the following case:<br/>
   * Compressing with gzip before receiving data <br/>
 
In order to make the examples more readable and more compact, parts of the source code retrieved from the original applications have either been removed or commented. 
For every case in every energy pattern, there is an example under a directory with the same name. 