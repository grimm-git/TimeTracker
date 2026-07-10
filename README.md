# TimeTracker

Timetracker is a tool that records your working time.

It automatically starts a session, when it is started and ends a session when it receives the terminate signal from the operating system, which caused a gracefull exit.

If you close the main window by the "close" button ot the "x" in the headline of the window, only the main windows will be closed but the application continues to operate in the background. Pressing the hotkey (Default: CTRL + SHIFT + F10) brings the main window back on screen.

The ideal configuration is to start TimeTracker in the autostart procedure of your computer. When you shut down yaour computer it sends the terminate singnal to TimeTracker which closes the active session and exits gracefully.

If, for any reason, TimeTracker terminate ungracefully, caused by a crash or a kill signal, the current session remain unfinished. With the next start TimeTracker detects this and asks how to continue. There are two posibilities:
  - Continue the session
  - Finish it and start a new session


## build packages for Linux or Windows
The build chain supports multi-system packages but not at once
  - gradlew createZip
    Create a zip package for Linux, jlink package includes the JDK
  - gradlew createZip -Pplatform=win
    Create a zip package for Windows, jlink package includes the JDK
  - gradlew createExe
    Create an executable for Windows containing the Java app
    Requires an JDK installed on the target system

