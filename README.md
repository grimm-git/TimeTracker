# TimeTracker

Timetracker is a tool that records your working time.

The tool is similar to the TimeTool from FaJo, which supports tracking and analysis of daily work time. In 2015 development and maintenance was stopped. Since then it can be used as it is. Nevertheless, in the last eleven years TimeTool and Windows develop a kind of love-hate relationship and with each Windows update it tends more towards hate. Therefore, I started this development to allow the TimeTool to retire, at least in my use case.

TimeTracker helps you to automatically record your daily work time, especially if your main tool is a computer. It automatically starts a session, when it is started and ends a session when it receives the terminate signal from the operating system, which caused a gracefull exit.

If you close the main window by the "close" button ot the "x" in the headline of the window, only the main windows will be closed but the application continues to operate in the background. Pressing the hotkey (Default: CTRL + SHIFT + F10) brings the main window back on screen.

The ideal configuration is to start TimeTracker in the autostart procedure of your computer. When you shut down your computer it sends the termination singnal to TimeTracker which closes the active session and exits gracefully.

TimeTracker assumes that start a working day and ends it on the same day. Therefore, if TimeTracker is started and detects a session already created for that day, it will ask what to do
  - Continue the session or
  - Finish it and start a new session

There is much more to say. It is not as feature-rich as FaJo's TimeTool, but will fulfill my use case.

Have fun and enjoy

Matthias


## Build packages for Linux or Windows
The build chain supports multi-system packages but not at once
  - gradlew createZip
  - gradlew createZip -Pplatform=win
  - gradlew createExe

**createZip** creates a zip package for Linux with jlink, the package includes the JDK.
With the parameter **-Pplatform=win**, instead it creates a zip package for Windows.

**createExe** on the other hand, creates an executable for Windows containing the Java application and all resources and libs, but it requires an JDK installed on the target system.


  