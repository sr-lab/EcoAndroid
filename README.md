# Refactor4Green
An Android Studio Plugin for Reducing Energy Consumption of Java Mobile Applications through Automated Refactoring.
<br/> 
The energy patterns the plugin can detect are: <br/>
  1. *Dynamic Retry Delay*, with the following cases:<br/>
   * Switching the time a thread goes to sleep from constant to dynamic (only considering the cases where variables are used to put the thread to sleep)<br/>
   * Checking the network connection before processing a thread <br/>
