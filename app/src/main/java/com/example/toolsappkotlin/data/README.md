# Physics Tools App

Physics Tools App is an Android class project written in Kotlin. It currently
uses the device's accelerometer to display live acceleration, record readings,
and view saved recordings. The screens use XML layouts, and local data is stored
in SQLite through the Room library.

## Using the app

1. Open the app and tap **Go to Accelerometer**.
2. Tap **Resume live readings** to display acceleration along the X, Y, and Z
   axes. Values are shown in meters per second squared (m/s²) and include gravity.
3. Tap **Start recording** to begin a new recording. This also starts live
   readings if they are paused.
4. Tap **Stop recording** to finish the recording and display the recordings list.
   Pausing live readings or leaving the screen also stops recording.
5. Tap **View recordings** to refresh the list of saved recordings.
6. Tap a recording to view its readings. Use **Previous 100** and **Next 100** to
   browse longer recordings, or **Back to recordings** to return to the list.

## How recording works

Each Start/Stop period is a separate recording with its own ID, start time, and
stop time. The recordings list shows the newest recordings first, with their time
range, duration, and number of readings.

While recording is active, the app saves each accelerometer event delivered by
Android. Each reading contains the X, Y, and Z values, a date/time timestamp, the
original sensor timestamp, and the ID of its recording. Values are stored at
full precision and rounded only for display.

The app requests Android's normal sensor update rate. The actual interval
between readings depends on the phone or emulator; it is not a fixed interval
set by the app.

Database operations run on a background worker in order, keeping storage work
separate from screen updates. Opening a recording shows its readings from oldest
to newest, with 100 readings per page.

## Local storage

The shared `AppDatabase` instance opens `physics_tools.db` on the device. It has
two tables:

- `accelerometer_recordings`: each recording's ID, start time, and stop time.
- `accelerometer_readings`: the measurements and timestamps linked to a recording.

Saved recordings remain available after restarting the app or emulator. Each
phone or emulator has its own database; recordings are not shared.

Recording does not continue in the background. If the app is abruptly terminated,
queued readings may be lost and the recording may appear as incomplete because
no stop time was saved.
