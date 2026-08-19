# Utilities

This folder contains auxiliary tools and scripts that might be helpful to developers working with Android applications.

## Android Memory Monitor (`android_mem_monitor.py`)

The Android Memory Monitor is a command-line and web dashboard tool for continuous monitoring of an Android app's memory footprint and process state (OOM Score Adjustment) over time.
The dashboard exports a text file with the OOM score change history and max memory usage per process when exiting the dashboard.

### Features
*   **Live Terminal Dashboard**: View OOM scores, Process States, and memory footprints directly in the console.
*   **Web Dashboard**: Access an interactive web interface (e.g., `http://localhost:8080`) featuring live memory trend charts, historical OOM logs, and the ability to save standalone HTML snapshots for sharing.

### Prerequisites
*   **Python 3.x** installed on your host machine.
*   **ADB (Android Debug Bridge)** installed and added to your PATH.
*   An Android device connected via USB or Wi-Fi with **USB Debugging enabled**.

### Usage

Run the script from your terminal, passing the target application's package name as an argument:

```bash
python3 android_mem_monitor.py <package_name>
```

**Example:**
```bash
python3 android_mem_monitor.py com.example.mygame
```

**Options:**
*   `--dump200`: If specified, the script will automatically take a `dumpsys activity` snapshot whenever the OOM score hits 200 (Perceptible Background). This is particularly useful for debugging why an app isn't fully suspending.

```bash
python3 android_mem_monitor.py com.example.mygame --dump200
```
