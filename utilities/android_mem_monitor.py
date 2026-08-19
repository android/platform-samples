#!/usr/bin/env python3
#
# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Android Memory Monitor

A command-line and web dashboard tool for continuous monitoring of an Android
app's
memory footprint and process state (OOM Score Adjustment) over time.

This script uses `adb shell` to query:
- `dumpsys activity`: To find process IDs and active components (Activities,
Services).
- `/proc/[pid]/oom_score_adj`: To track the Out-Of-Memory (OOM) score
adjustment, which dictates the OS kill priority.
- `/proc/[pid]/status`: To track Anonymous RSS and Swap memory usage
continuously.

Prerequisites:
- Python 3.x installed on your host machine.
- ADB (Android Debug Bridge) installed and added to your PATH.
- An Android device connected via USB or Wi-Fi with USB Debugging enabled.

Features:
- Live Terminal Dashboard: View OOM scores, Proc States, and memory footprints
directly in the console.
- Web Dashboard: Access an interactive web interface (e.g.,
http://localhost:8080) featuring live memory trend charts, historical OOM logs,
and the ability to save standalone HTML snapshots for sharing.

Usage:
  python3 android_mem_monitor.py [package_name] [--dump200]

Example:
  python3 android_mem_monitor.py com.example.mygame
  python3 android_mem_monitor.py com.example.mygame --dump200

Options:
  --dump200   If specified, the script will automatically take a `dumpsys
  activity` snapshot
              whenever the OOM score hits 200 (Perceptible Background), which is
              useful for
              debugging why an app isn't fully suspending.
"""

import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import os
import re
import shutil
import subprocess
import sys
import textwrap
import threading
import time

APP_START_TIME = time.time()
ENABLE_DUMPSYS = False
WEB_DASHBOARD_URL = None
args = sys.argv[1:]
if "--dump200" in args:
  ENABLE_DUMPSYS = True
  args.remove("--dump200")

if len(args) > 0:
  PACKAGE_NAME = args[0]
else:
  PACKAGE_NAME = input("Enter the package name to monitor: ").strip()
  if not PACKAGE_NAME:
    print("\033[1;31mError: Package name cannot be empty.\033[0m")
    sys.exit(1)

LOG_FILENAME = (
    f"mem_logs_{PACKAGE_NAME.replace('.', '_')}_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
)

pids = []
pid_stats = {}
logs = []
lock = threading.Lock()

PID_COLORS = [
    "\033[30;48;5;226m",  # Yellow
    "\033[30;48;5;211m",  # Pink
    "\033[30;48;5;81m",  # Light Blue
    "\033[30;48;5;204m",  # Bright Red/Magenta
    "\033[30;48;5;15m",  # White
    "\033[30;48;5;121m",  # Pale Mint
    "\033[30;48;5;214m",  # Orange
    "\033[30;48;5;118m",  # Lime Green
    "\033[30;48;5;45m",  # Turquoise
    "\033[30;48;5;135m",  # Lavender/Purple
]
color_lock = threading.Lock()
pid_color_map = {}
next_color_idx = 0


def get_pid_color(pid_str):
  global next_color_idx
  with color_lock:
    if pid_str not in pid_color_map:
      pid_color_map[pid_str] = PID_COLORS[next_color_idx % len(PID_COLORS)]
      next_color_idx += 1
    return pid_color_map[pid_str]


PROC_STATES = {
    -1: "UNKNOWN",
    0: "PERSISTENT",
    1: "PERSISTENT_UI",
    2: "TOP",
    3: "BOUND_TOP",
    4: "FOREGROUND_SERVICE",
    5: "BOUND_FOREGROUND_SERVICE",
    6: "IMPORTANT_FOREGROUND",
    7: "IMPORTANT_BACKGROUND",
    8: "TRANSIENT_BACKGROUND",
    9: "BACKUP",
    10: "SERVICE",
    11: "RECEIVER",
    12: "TOP_SLEEPING",
    13: "HEAVY_WEIGHT",
    14: "HOME",
    15: "LAST_ACTIVITY",
    16: "CACHED_ACTIVITY",
    17: "CACHED_ACTIVITY_CLIENT",
    18: "CACHED_RECENT",
    19: "CACHED_EMPTY",
    20: "NONEXISTENT",
}


def map_oom_details(oom_adj):
  """Maps an OOM score adjustment integer to its Android constant name and a human-readable description.

  Args:
      oom_adj (int/str): The OOM score adjustment value.

  Returns:
      tuple: (constant_name, description)
  """
  if oom_adj is None or str(oom_adj) == "...":
    return ("...", "...")
  try:
    val = int(oom_adj)
  except ValueError:
    return ("...", "...")

  constant = "UNKNOWN"
  desc = "Unknown"

  # Constant & Description
  if val == 0:
    constant, desc = "FOREGROUND_APP_ADJ", "Foreground (focused/visible)"
  elif val == 50:
    constant, desc = "RECENT_FOREGROUND", "Grace period buffer"
  elif 100 <= val <= 199:
    constant, desc = "VISIBLE_APP_ADJ", "Visible but unfocused"
  elif val == 200:
    constant, desc = "PERCEPTIBLE_APP_ADJ", "Primary Perceptible Svc"
  elif 201 <= val <= 224:
    constant, desc = "PERCEPTIBLE_LESS_IMPORTANT", "Bound offsets"
  elif val == 225:
    constant, desc = "PERCEPTIBLE_MEDIUM_APP_ADJ", "Medium-priority bindings"
  elif 226 <= val <= 249:
    constant, desc = "PERCEPTIBLE_LESS_IMPORTANT", "Bound offsets"
  elif val == 250:
    constant, desc = "PERCEPTIBLE_LOW_APP_ADJ", "Low-priority bindings"
  elif 300 <= val <= 399:
    constant, desc = "BACKUP_APP_ADJ", "Cloud save/restore"
  elif 400 <= val <= 499:
    constant, desc = "HEAVY_WEIGHT", "cantSaveState app"
  elif 500 <= val <= 599:
    constant, desc = "SERVICE_ADJ", "Standard BG service"
  elif 700 <= val <= 799:
    constant, desc = "PREVIOUS_FOREGROUND_APP_ADJ", "Last active app"
  elif 800 <= val <= 899:
    constant, desc = "SERVICE_LESS_IMPORTANT_ADJ", "Minor BG service"
  elif val >= 900:
    constant, desc = "CACHED_APP_ADJ", "Cached, idle, frozen"

  return constant, desc


def plot_ascii_graph(data, height=5, width=60):
  """Generates an ASCII sparkline graph for the provided numerical data.

  Args:
      data (list of float): The memory data points to plot.
      height (int): The height of the graph in characters.
      width (int): The width (number of data points) to display.

  Returns:
      list of str: A list of strings representing the lines of the graph.
  """
  if not data:
    return ["  No data yet"]

  min_val, max_val = min(data), max(data)
  if max_val == min_val:
    max_val += 1.0  # prevent division by zero
    min_val -= 1.0

  range_val = max_val - min_val
  lines = []

  plot_data = data[-width:]

  for row in range(height - 1, -1, -1):
    line = ""
    row_min = min_val + (row / height) * range_val
    row_max = min_val + ((row + 1) / height) * range_val

    for val in plot_data:
      if val >= row_max:
        line += "█"
      elif val > row_min:
        ratio = (val - row_min) / (row_max - row_min)
        chars = " ▂▃▄▅▆▇█"
        idx = int(ratio * 7)
        line += chars[idx]
      else:
        line += " "

    y_label = f"{row_max:7.1f} MB |"
    lines.append(f"\033[1;36m{y_label}\033[0m {line}")

  # X axis
  lines.append(" " * 11 + "+-" + "-" * len(plot_data))
  return lines


def get_pids():
  """Finds all active Process IDs (PIDs) associated with the target package name.

  Uses `adb shell dumpsys activity p [package]` and parses the "PID mappings:"
  block.
  This approach ensures we catch all isolated processes (like WebViews) that
  might
  run under the same package but not the same exact process name.
  """
  try:
    # Use dumpsys to get ALL processes tied to the app, including isolated WebViews
    out = subprocess.check_output(
        ["adb", "shell", "dumpsys", "activity", "p", PACKAGE_NAME],
        text=True,
        stderr=subprocess.DEVNULL,
    )
    found_pids = {}
    in_pid_mappings = False

    for line in out.split("\n"):
      line = line.strip()
      # The "PID mappings:" section lists all processes currently mapped to the package.
      if line == "PID mappings:":
        in_pid_mappings = True
        continue
      # End of the mappings section is typically an empty line.
      elif in_pid_mappings and line == "":
        break

      if in_pid_mappings and line.startswith("PID #"):
        # Parse lines like: "PID #30749: ProcessRecord{... 30749:com.google.android.webview...}"
        try:
          pid_part = line.split(":")[0]
          pid = pid_part.replace("PID #", "").strip()
          name = line.split(":")[-1].replace("}", "").strip()
          found_pids[pid] = name
        except:
          pass

    return found_pids
  except:
    return {}


def get_active_components():
  """Parses `dumpsys activity p [package]` to extract the currently active components

  (Activities, Services, Providers, Receivers) and the overall 'curProcState'
  for each PID.

  This provides context as to *why* a process might have a specific OOM score.
  """
  try:
    # Check dumpsys for detailed process records
    out = subprocess.check_output(
        ["adb", "shell", "dumpsys", "activity", "p", PACKAGE_NAME],
        text=True,
        stderr=subprocess.DEVNULL,
    )
    components_by_pid = {}
    current_pid = None
    current_section = None

    for line in out.split("\n"):
      line_stripped = line.strip()

      # Detect a new process block. Looks like: "* APP.* ProcessRecord{... pid:pkg_name}"
      if "ProcessRecord{" in line and ":" in line:
        try:
          pid_str = line.split("ProcessRecord{")[1].split(":")[0].split()[-1]
          current_pid = pid_str
          if current_pid not in components_by_pid:
            components_by_pid[current_pid] = {
                "Services": [],
                "Activities": [],
                "Providers": [],
                "Receivers": 0,
                "proc_state": "UNKNOWN",
            }
          current_section = None
        except:
          current_pid = None
      elif current_pid:
        # Extract the current internal process state (integer mapping to PROC_STATES)
        if line_stripped.startswith("curProcState="):
          try:
            state_val = int(line_stripped.split("curProcState=")[1].split()[0])
            components_by_pid[current_pid]["proc_state"] = PROC_STATES.get(
                state_val, f"UNKNOWN({state_val})"
            )
          except:
            pass

        # State machine to track which component section we are currently parsing
        elif line_stripped.startswith("Activities:"):
          current_section = "Activities"
        elif line_stripped.startswith("Services:"):
          current_section = "Services"
        elif line_stripped.startswith("Published Providers:"):
          current_section = "Providers"
        elif line_stripped.startswith("mReceivers:"):
          current_section = "Receivers"
        # Sections that break us out of parsing components
        elif (
            line_stripped.startswith("mConnections:")
            or line_stripped.startswith("Connected Providers:")
            or line_stripped.startswith("OOM levels:")
        ):
          current_section = None

        # Parse the actual components listed under a section
        elif line_stripped.startswith("- ") and current_section:
          if current_section in ("Activities", "Services"):
            for p in line_stripped.split():
              if "/" in p and PACKAGE_NAME in p:
                name = p.split("/")[-1].split(".")[-1]
                if name not in components_by_pid[current_pid][current_section]:
                  components_by_pid[current_pid][current_section].append(name)
                break
          elif current_section == "Providers":
            if not line_stripped.startswith(
                "- >"
            ) and not line_stripped.startswith("->"):
              name = line_stripped[2:].strip().split(".")[-1]
              if name not in components_by_pid[current_pid]["Providers"]:
                components_by_pid[current_pid]["Providers"].append(name)
          elif current_section == "Receivers":
            components_by_pid[current_pid]["Receivers"] += 1

    return components_by_pid
  except:
    return {}


def update_pids_loop():
  """Background thread loop that periodically (every 2s) refreshes the list of active PIDs

  and their active components using dumpsys.
  """
  global pids
  while True:
    new_pids_dict = get_pids()
    active_comps = get_active_components()

    # Thread Safety: Lock to safely update the shared global state
    # that is read by other polling and drawing threads.
    with lock:
      pids = list(new_pids_dict.keys())
      for pid, name in new_pids_dict.items():
        if pid not in pid_stats:
          pid_stats[pid] = {"history": []}
        pid_stats[pid]["name"] = name
        pid_stats[pid]["components"] = active_comps.get(pid, {})
        pid_stats[pid]["last_comps_update"] = time.time()
    time.sleep(2)


def poll_oom_loop():
  """Background thread loop that continuously reads the `oom_score_adj` for all tracked PIDs.

  It launches a single `adb shell` process that runs a tiny bash loop on the
  device.
  This is orders of magnitude faster and less resource-intensive than repeatedly
  calling `adb shell` from Python for every check.
  """
  while True:
    with lock:
      running_pids = list(pids)

    if not running_pids:
      time.sleep(1)
      continue

    # The bash loop running directly on the Android device:
    # Repeatedly reads /proc/[pid]/oom_score_adj for all active PIDs and echos the result.
    cmd = (
        "while true; do for p in "
        + " ".join(running_pids)
        + '; do echo -n "$p:"; cat /proc/$p/oom_score_adj 2>/dev/null || echo'
        ' "ERR"; done; sleep 0.01; done'
    )

    process = subprocess.Popen(
        ["adb", "shell", cmd], stdout=subprocess.PIPE, text=True
    )

    try:
      for line in process.stdout:
        line = line.strip()
        if not line:
          continue

        # Process the output which is in the format "pid:score"
        parts = line.split(":")
        if len(parts) == 2:
          pid, val_str = parts
          val_str = val_str.strip()
          if val_str != "ERR" and (
              val_str.isdigit()
              or (val_str.startswith("-") and val_str[1:].isdigit())
          ):
            val = int(val_str)
            with lock:
              if pid not in pid_stats:
                pid_stats[pid] = {"history": []}
              old_val = pid_stats[pid].get("oom_adj")
              if old_val != val:
                ts = datetime.datetime.now().strftime("%H:%M:%S.%f")[:-3]
                loop_start_time = time.time()
                pid_stats[pid]["last_oom_change"] = loop_start_time

                if "oom_max_mem" not in pid_stats[pid]:
                  pid_stats[pid]["oom_max_mem"] = {}

                last_mem = 0.0
                if "history" in pid_stats[pid] and pid_stats[pid]["history"]:
                  last_mem = pid_stats[pid]["history"][-1]

                # Track the maximum memory observed during this new OOM score state,
                # ensuring we have a baseline initialized.
                if val not in pid_stats[pid]["oom_max_mem"]:
                  pid_stats[pid]["oom_max_mem"][val] = last_mem
                else:
                  pid_stats[pid]["oom_max_mem"][val] = max(
                      pid_stats[pid]["oom_max_mem"][val], last_mem
                  )

                duration_msg = ""
                if (
                    old_val is not None
                    and "last_changed_time" in pid_stats[pid]
                ):
                  last_changed = pid_stats[pid]["last_changed_time"]
                  duration_msg = (
                      f" (Spent {(loop_start_time - last_changed)*1000:.0f}ms"
                      f" in {old_val})"
                  )

                plain_msg = (
                    f"[{ts}] PID {pid} oom_score_adj changed:"
                    f" {old_val if old_val is not None else 'Initial'} ->"
                    f" {val}{duration_msg}"
                )
                color_msg = (
                    f"[{ts}] {get_pid_color(pid)}PID {pid}\033[0m oom_score_adj"
                    f" changed: {old_val if old_val is not None else 'Initial'}"
                    f" -> {val}{duration_msg}"
                )
                logs.insert(0, color_msg)
                with open(LOG_FILENAME, "a") as f:
                  f.write(plain_msg + "\n")

                pid_stats[pid]["last_changed_time"] = loop_start_time

                if val == 200 and ENABLE_DUMPSYS:

                  def dump_state(target_pid, time_str):
                    try:
                      dump_out = subprocess.check_output(
                          [
                              "adb",
                              "shell",
                              "dumpsys",
                              "activity",
                              "processes",
                              PACKAGE_NAME,
                          ],
                          text=True,
                          stderr=subprocess.DEVNULL,
                      )
                      fname = (
                          f"dumpsys_200_{target_pid}_{time_str.replace(':', '')}.txt"
                      )
                      with open(fname, "w") as df:
                        df.write(dump_out)
                      with lock:
                        logs.insert(0, f"[*] Saved dumpsys snapshot to {fname}")
                    except Exception:
                      pass

                  threading.Thread(
                      target=dump_state, args=(pid, ts), daemon=True
                  ).start()

              pid_stats[pid]["oom_adj"] = val

        with lock:
          if list(pids) != running_pids:
            break
    finally:
      process.terminate()
      process.wait()


def poll_meminfo_loop():
  """Background thread loop that continuously reads memory stats from `/proc/[pid]/status`.

  Similar to the OOM loop, it pushes a while loop down to the adb shell to
  stream
  the contents of the status file back to Python. It parses out `RssAnon` and
  `VmSwap`,
  which together represent the "Gold Standard" for measuring a background
  process's memory footprint.
  """
  last_history_time = time.time()
  history_accumulators = {}

  while True:
    with lock:
      running_pids = list(pids)

    if not running_pids:
      time.sleep(0.5)
      continue

    # The bash loop on the device: Echos PID, cats status, and outputs a separator '---'
    cmd = (
        "while true; do for p in "
        + " ".join(running_pids)
        + '; do echo "PID:$p"; cat /proc/$p/status 2>/dev/null; done; echo'
        ' "---"; sleep 0.1; done'
    )

    process = subprocess.Popen(
        ["adb", "shell", cmd], stdout=subprocess.PIPE, text=True
    )

    current_reading_pid = None
    anon_rss_mb = 0.0
    swap_mb = 0.0
    has_rss = False
    has_swap = False

    last_loop_time = time.time()

    try:
      for line in process.stdout:
        line = line.strip()
        if not line:
          continue

        if line.startswith("PID:"):
          # We hit a new PID block. Save the aggregated stats for the previous PID we were reading.
          if current_reading_pid is not None and (has_rss or has_swap):
            total_mb = anon_rss_mb + swap_mb
            if current_reading_pid not in history_accumulators:
              history_accumulators[current_reading_pid] = []
            history_accumulators[current_reading_pid].append(total_mb)

            with lock:
              if current_reading_pid not in pid_stats:
                pid_stats[current_reading_pid] = {"history": []}
              pid_stats[current_reading_pid][
                  "anon_rss"
              ] = f"{anon_rss_mb:.2f} MB"
              pid_stats[current_reading_pid]["swap"] = f"{swap_mb:.2f} MB"
              pid_stats[current_reading_pid]["_latest_total_mb"] = total_mb

          current_reading_pid = line.split(":")[1]
          anon_rss_mb = 0.0
          swap_mb = 0.0
          has_rss = False
          has_swap = False

        elif line.startswith("RssAnon:"):
          try:
            anon_rss_mb = int(line.split()[1]) / 1024.0
            has_rss = True
          except:
            pass
        elif line.startswith("VmSwap:"):
          try:
            swap_mb = int(line.split()[1]) / 1024.0
            has_swap = True
          except:
            pass
        elif line == "---":
          # Reached the separator at the end of the current loop iteration.
          # Save stats for the last PID read in this cycle.
          if current_reading_pid is not None and (has_rss or has_swap):
            total_mb = anon_rss_mb + swap_mb
            if current_reading_pid not in history_accumulators:
              history_accumulators[current_reading_pid] = []
            history_accumulators[current_reading_pid].append(total_mb)

            with lock:
              if current_reading_pid not in pid_stats:
                pid_stats[current_reading_pid] = {"history": []}
              pid_stats[current_reading_pid][
                  "anon_rss"
              ] = f"{anon_rss_mb:.2f} MB"
              pid_stats[current_reading_pid]["swap"] = f"{swap_mb:.2f} MB"
              pid_stats[current_reading_pid]["_latest_total_mb"] = total_mb

          current_reading_pid = None
          has_rss = False
          has_swap = False

          current_time = time.time()
          delta_t = current_time - last_loop_time
          last_loop_time = current_time

          with lock:
            for p in running_pids:
              if p in pid_stats and "_latest_total_mb" in pid_stats[p]:
                t_mb = pid_stats[p]["_latest_total_mb"]

                if "oom_max_mem" not in pid_stats[p]:
                  pid_stats[p]["oom_max_mem"] = {}
                if "oom_mem_histogram" not in pid_stats[p]:
                  pid_stats[p]["oom_mem_histogram"] = {}

                current_oom = pid_stats[p].get("oom_adj")
                if current_oom is not None:
                  if current_oom not in pid_stats[p]["oom_mem_histogram"]:
                    pid_stats[p]["oom_mem_histogram"][current_oom] = {}

                  bucket = round(t_mb, 1)
                  hist = pid_stats[p]["oom_mem_histogram"][current_oom]
                  hist[bucket] = hist.get(bucket, 0.0) + delta_t

                  prev_max = pid_stats[p]["oom_max_mem"].get(current_oom, 0)
                  if t_mb > prev_max:
                    pid_stats[p]["oom_max_mem"][current_oom] = t_mb

            if current_time - last_history_time >= 1.0:
              for p in running_pids:
                if p in history_accumulators and history_accumulators[p]:
                  avg_mb = sum(history_accumulators[p]) / len(
                      history_accumulators[p]
                  )
                  if p not in pid_stats:
                    pid_stats[p] = {"history": []}
                  if "history" not in pid_stats[p]:
                    pid_stats[p]["history"] = []

                  pid_stats[p]["history"].append(avg_mb)
                  if len(pid_stats[p]["history"]) > 60:
                    pid_stats[p]["history"].pop(0)

                  history_accumulators[p] = []
              last_history_time = current_time

            if list(pids) != running_pids:
              break
    except Exception:
      pass
    finally:
      process.terminate()
      process.wait()


def print_dashboard():
  """Renders the live terminal dashboard.

  Uses ANSI escape codes to clear the screen, draw colored text, and render
  ASCII graphs. Called periodically on the main thread.
  """
  with lock:
    current_pids = list(pids)
    current_stats = {k: dict(v) for k, v in pid_stats.items()}
    current_logs = list(logs[:200])

  out = []
  term_width = shutil.get_terminal_size().columns

  out.append(f"\033[1;36mAndroid Memory Monitor\033[0m")

  elapsed = int(time.time() - APP_START_TIME)
  m, s = divmod(elapsed, 60)
  h, m = divmod(m, 60)
  if h > 0:
    timer_str = f"{h:02d}:{m:02d}:{s:02d}"
  else:
    timer_str = f"{m:02d}:{s:02d}"

  out.append(
      f"Monitoring: \033[1m{PACKAGE_NAME}\033[0m  |  Elapsed:"
      f" \033[1;36m{timer_str}\033[0m"
  )
  if WEB_DASHBOARD_URL:
    out.append(f"Web Dashboard: \033[1;32m{WEB_DASHBOARD_URL}\033[0m")
  out.append("")
  out.append("=" * term_width)

  pid_col_width = 25
  if current_pids:
    for pid in current_pids:
      name = current_stats.get(pid, {}).get("name", "Unknown")
      if name == PACKAGE_NAME:
        display_name = "Main"
      elif name.startswith(PACKAGE_NAME + ":"):
        display_name = name[len(PACKAGE_NAME) :]
      else:
        display_name = name
      pid_col_width = max(pid_col_width, len(f"{pid} ({display_name})"))

  out.append(
      f"{'PID (Name)':<{pid_col_width}} | {'OOM Score':<65} | {'Anon RSS':<10}"
      f" | {'Swap':<10} | Proc State"
  )
  out.append("-" * term_width)

  if not current_pids:
    out.append("\033[3mWaiting for process to start...\033[0m")
  else:
    for pid in current_pids:
      st = current_stats.get(pid, {})
      oom = str(st.get("oom_adj", "..."))
      rss = st.get("anon_rss", "...")
      swap = st.get("swap", "...")

      oom_colored = oom
      if oom.lstrip("-").isdigit():
        val = int(oom)
        if val >= 900:
          oom_colored = f"\033[1;31m{oom}\033[0m"
        elif val >= 500:
          oom_colored = f"\033[1;33m{oom}\033[0m"
        else:
          oom_colored = f"\033[1;32m{oom}\033[0m"

      constant, desc = map_oom_details(oom)

      comps = st.get("components", {})
      proc_state = comps.get("proc_state", "UNKNOWN")

      last_oom = st.get("last_oom_change", 0)
      last_comps = st.get("last_comps_update", 0)

      if last_oom > last_comps:
        proc_state = f"\033[90m{proc_state} (syncing...)\033[0m"

      name = st.get("name", "Unknown")
      if name == PACKAGE_NAME:
        display_name = "Main"
      elif name.startswith(PACKAGE_NAME + ":"):
        display_name = name[len(PACKAGE_NAME) :]
      else:
        display_name = name

      pid_col = f"{pid} ({display_name})"
      pid_col_pad = " " * max(0, pid_col_width - len(pid_col))
      pid_col_colored = f"{get_pid_color(pid)}{pid_col}\033[0m{pid_col_pad}"

      raw_oom = f"{oom} ({constant}): {desc}"
      pad = " " * max(0, 65 - len(raw_oom))
      oom_col = f"{oom_colored} ({constant}): {desc}{pad}"

      out.append(
          f"{pid_col_colored} | {oom_col} | {rss:<10} | {swap:<10} |"
          f" {proc_state}"
      )

      comps = st.get("components", {})

      parts = []
      for label, key in [
          ("Activities", "Activities"),
          ("Services", "Services"),
          ("Providers", "Providers"),
      ]:
        items = comps.get(key)
        if items:
          val = ", ".join(items) if isinstance(items, list) else str(items)
          parts.append((label, val))
      if comps.get("Receivers", 0) > 0:
        parts.append(("Receivers", str(comps["Receivers"])))

      if parts:
        lines = []
        current_line = "  "
        current_line_visible = 2

        for i, (label, val) in enumerate(parts):
          if i > 0:
            current_line += " \033[1;30m|\033[0m "
            current_line_visible += 3

          current_line += f"\033[38;5;214m{label}:\033[0m "
          current_line_visible += len(label) + 2

          words = val.split(" ")
          for j, word in enumerate(words):
            word_len = len(word)
            if (
                current_line_visible + word_len > term_width - 2
                and current_line_visible > 2
            ):
              lines.append(current_line)
              current_line = "  "
              current_line_visible = 2

            current_line += f"\033[3m{word}\033[0m"
            current_line_visible += word_len

            if j < len(words) - 1:
              current_line += " "
              current_line_visible += 1

        if current_line.strip():
          lines.append(current_line)

        out.extend(lines)

  out.append("")
  out.append("=" * term_width)
  out.append("\033[1;35mMemory Trends (Anon RSS + Swap)\033[0m")

  if not current_pids:
    out.append("  Waiting for processes...")
  else:
    graph_blocks = []
    for pid in current_pids:
      st = current_stats.get(pid, {})
      history = st.get("history", [])
      name = st.get("name", "Unknown")

      block = []
      block.append(f"  \033[1m{get_pid_color(pid)}PID {pid}\033[0m ({name})")
      # Width=60 takes up ~75 characters including the Y-axis labels.
      for graph_line in plot_ascii_graph(history, width=60):
        block.append(f"  {graph_line}")
      graph_blocks.append(block)

    col_width = 90
    for i in range(0, len(graph_blocks), 2):
      out.append("")
      left_block = graph_blocks[i]
      right_block = graph_blocks[i + 1] if i + 1 < len(graph_blocks) else []

      max_lines = max(len(left_block), len(right_block))
      for j in range(max_lines):
        left_str = left_block[j] if j < len(left_block) else ""
        right_str = right_block[j] if j < len(right_block) else ""

        visible_len = len(re.sub(r"\033\[.*?m", "", left_str))
        pad = " " * max(0, col_width - visible_len)

        out.append(left_str + pad + right_str)

  out.append("")
  out.append("=" * term_width)

  term_height = shutil.get_terminal_size().lines
  lines_used = len(out) + 2
  max_lines = max(0, term_height - lines_used - 1)

  left_header = "\033[1;32mOOM Score Logs (latest first):\033[0m"
  right_header = "\033[1;32mMax Memory per OOM Score:\033[0m"

  visible_left_header = len(re.sub(r"\033\[.*?m", "", left_header))
  max_log_width = visible_left_header

  for i in range(min(max_lines, len(current_logs))):
    log_len = len(re.sub(r"\033\[.*?m", "", current_logs[i]))
    if log_len > max_log_width:
      max_log_width = log_len

  pid_col_width = 12
  right_lines_data = []

  for pid in sorted(current_pids, key=lambda x: int(x)):
    st = current_stats.get(pid, {})
    name = st.get("name", "Unknown")
    if name == PACKAGE_NAME:
      display_name = "Main"
    elif name.startswith(PACKAGE_NAME + ":"):
      display_name = name[len(PACKAGE_NAME) :]
    else:
      display_name = name

    raw_pid_col = f"{pid} ({display_name})"
    pid_col_width = max(pid_col_width, len(raw_pid_col))
    pid_col = f"{get_pid_color(pid)}{raw_pid_col}\033[0m"

    oom_max_mem = st.get("oom_max_mem", {})
    oom_mem_histogram = st.get("oom_mem_histogram", {})
    for oom in sorted(oom_max_mem.keys()):
      max_mem = oom_max_mem[oom]
      threshold = 0.95 * max_mem

      hist = oom_mem_histogram.get(oom, {})
      time_spent = sum(dur for b, dur in hist.items() if b >= threshold)
      total_oom_time = sum(hist.values())

      val_str = f"{max_mem:.2f} MB (>{threshold:.1f} MB for {time_spent:.1f}s)"
      oom_str = f"{oom} ({total_oom_time:.1f}s)"
      right_lines_data.append((pid_col, oom_str, val_str))

  table_width = (
      pid_col_width + 3 + 16 + 3 + 35
  )  # enough for "123.45 MB (>117.2 MB for 123.4s)"

  left_col_width = max_log_width + 4
  if left_col_width + table_width > term_width:
    left_col_width = max(30, term_width - table_width - 2)

  pad = " " * max(0, left_col_width - visible_left_header)
  out.append(left_header + pad + right_header)

  right_lines = []
  right_lines.append(
      f"{'PID (Name)':<{pid_col_width}} | {'OOM Score (Time)':<16} |"
      f" {'Max Memory Observed'}"
  )
  right_lines.append("-" * max(30, table_width))

  for pid_col, oom_str, val_str in right_lines_data:
    visible_pid_col = len(re.sub(r"\033\[.*?m", "", pid_col))
    pid_pad = " " * max(0, pid_col_width - visible_pid_col)
    right_lines.append(f"{pid_col}{pid_pad} | {oom_str:<16} | {val_str}")

  for i in range(max_lines):
    left_str = current_logs[i] if i < len(current_logs) else ""
    right_str = right_lines[i] if i < len(right_lines) else ""

    visible_left = len(re.sub(r"\033\[.*?m", "", left_str))
    if visible_left > left_col_width - 2:
      left_str = left_str[: left_col_width - 5] + "..."
      visible_left = left_col_width - 2

    pad_len = left_col_width - visible_left
    pad = " " * max(0, pad_len)

    out.append(left_str + pad + right_str)

  out.append("")
  out.append(
      f"\033[3mPress Ctrl+C to exit. Logs are saved to {LOG_FILENAME}\033[0m"
  )

  while len(out) < term_height - 1:
    out.append("")

  sys.stdout.write("\033[H" + "\033[K\n".join(out) + "\033[K\033[J")
  sys.stdout.flush()


HTML_CONTENT = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Android Memory Monitor</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { background-color: #0f172a; color: #f8fafc; font-family: 'Inter', system-ui, sans-serif; margin: 0; padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; }
        .header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid #334155; margin-bottom: 20px; }
        .card { background: rgba(30, 41, 59, 0.7); border: 1px solid #334155; border-radius: 12px; padding: 20px; margin-bottom: 20px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); backdrop-filter: blur(10px); }
        h1 { margin: 0; font-size: 1.5rem; color: #38bdf8; }
        h2 { margin-top: 0; font-size: 1.25rem; }
        table { width: 100%; border-collapse: collapse; }
        th, td { text-align: left; padding: 12px; border-bottom: 1px solid #334155; }
        th { color: #94a3b8; font-weight: 500; }
        .logs { font-family: monospace; background: #020617; padding: 15px; border-radius: 8px; max-height: 300px; overflow-y: auto; color: #10b981; }
        .btn { background: #0ea5e9; color: white; border: none; padding: 10px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; transition: background 0.2s; }
        .btn:hover { background: #0284c7; }
        .charts-container { display: grid; grid-template-columns: repeat(auto-fit, minmax(500px, 1fr)); gap: 20px; }
        .chart-wrapper { position: relative; height: 300px; }
        .badge { padding: 4px 8px; border-radius: 9999px; font-size: 0.75rem; font-weight: bold; }
        .bg-green { background: rgba(16, 185, 129, 0.2); color: #34d399; }
        .bg-yellow { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
        .bg-red { background: rgba(239, 68, 68, 0.2); color: #f87171; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Android Memory Monitor</h1>
            <button class="btn" onclick="saveSnapshot()">💾 Save Snapshot</button>
        </div>
        
        <div class="card">
            <h2>Active Processes</h2>
            <table>
                <thead>
                    <tr>
                        <th>PID</th>
                        <th>Name</th>
                        <th>OOM Score</th>
                        <th>Anon RSS</th>
                        <th>Swap</th>
                        <th>State</th>
                    </tr>
                </thead>
                <tbody id="process-tbody"></tbody>
            </table>
        </div>

        <div class="charts-container" id="charts-container"></div>
        
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 20px;">
            <div class="card">
                <h2>OOM Score Logs</h2>
                <div class="logs" id="logs-container"></div>
            </div>
            <div class="card">
                <h2>Max Memory per OOM Score</h2>
                <table>
                    <thead>
                        <tr>
                            <th>PID</th>
                            <th>OOM Score (Time)</th>
                            <th>Max Memory</th>
                        </tr>
                    </thead>
                    <tbody id="oom-mem-tbody"></tbody>
                </table>
            </div>
        </div>
    </div>

    <script>
        let charts = {};
        
        async function fetchData() {
            if (window.STATIC_DATA) return window.STATIC_DATA;
            try {
                const res = await fetch('/api/data');
                return await res.json();
            } catch (e) {
                console.error("Failed to fetch data:", e);
                return null;
            }
        }
        
        function getOomBadge(val) {
            if (val === '...') return '<span class="badge" style="background:#334155;color:#cbd5e1">...</span>';
            val = parseInt(val);
            if (val >= 900) return `<span class="badge bg-red">${val}</span>`;
            if (val >= 500) return `<span class="badge bg-yellow">${val}</span>`;
            return `<span class="badge bg-green">${val}</span>`;
        }

        async function updateDashboard() {
            const data = await fetchData();
            if (!data) return;
            
            const tbody = document.getElementById('process-tbody');
            tbody.innerHTML = '';
            
            data.pids.forEach(pid => {
                const st = data.stats[pid];
                const tr = document.createElement('tr');
                const state = st.components?.proc_state || 'UNKNOWN';
                
                let compsStr = '';
                const comps = st.components || {};
                const parts = [];
                if (comps.Activities && comps.Activities.length > 0) parts.push(`<span style="color:#f59e0b">Activities:</span> <span style="font-style:italic">${comps.Activities.join(', ')}</span>`);
                if (comps.Services && comps.Services.length > 0) parts.push(`<span style="color:#f59e0b">Services:</span> <span style="font-style:italic">${comps.Services.join(', ')}</span>`);
                if (comps.Providers && comps.Providers.length > 0) parts.push(`<span style="color:#f59e0b">Providers:</span> <span style="font-style:italic">${comps.Providers.join(', ')}</span>`);
                if (comps.Receivers > 0) parts.push(`<span style="color:#f59e0b">Receivers:</span> <span style="font-style:italic">${comps.Receivers}</span>`);
                if (parts.length > 0) {
                    compsStr = parts.join(' | ');
                }

                tr.innerHTML = `
                    <td${compsStr ? ' style="border-bottom:none"' : ''}>${pid}</td>
                    <td${compsStr ? ' style="border-bottom:none"' : ''}>${st.name || 'Unknown'}</td>
                    <td${compsStr ? ' style="border-bottom:none"' : ''}>${getOomBadge(st.oom_adj ?? '...')}</td>
                    <td${compsStr ? ' style="border-bottom:none"' : ''}>${st.anon_rss || '...'}</td>
                    <td${compsStr ? ' style="border-bottom:none"' : ''}>${st.swap || '...'}</td>
                    <td${compsStr ? ' style="border-bottom:none"' : ''}>${state}</td>
                `;
                tbody.appendChild(tr);
                
                if (compsStr) {
                    const trComps = document.createElement('tr');
                    trComps.innerHTML = `<td colspan="6" style="padding-top: 0; padding-bottom: 12px; font-size: 0.85em; color: #94a3b8;">${compsStr}</td>`;
                    tbody.appendChild(trComps);
                }
            });
            
            const chartsContainer = document.getElementById('charts-container');
            data.pids.forEach(pid => {
                const st = data.stats[pid];
                if (!st.history || st.history.length === 0) return;
                
                let chartId = 'chart-' + pid;
                if (!charts[pid]) {
                    if (!document.getElementById(chartId)) {
                        const wrapper = document.createElement('div');
                        wrapper.className = 'card';
                        wrapper.innerHTML = `<h2>Memory Trend: PID ${pid} (${st.name || 'Unknown'})</h2><div class="chart-wrapper"><canvas id="${chartId}"></canvas></div>`;
                        chartsContainer.appendChild(wrapper);
                    }
                    
                    const ctx = document.getElementById(chartId).getContext('2d');
                    charts[pid] = new Chart(ctx, {
                        type: 'line',
                        data: {
                            labels: Array.from({length: 60}, (_, i) => i + 1),
                            datasets: [{
                                label: 'Memory (MB)',
                                data: [],
                                borderColor: '#38bdf8',
                                backgroundColor: 'rgba(56, 189, 248, 0.1)',
                                borderWidth: 2,
                                fill: true,
                                tension: 0.4,
                                pointRadius: 0,
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            animation: false,
                            scales: {
                                y: { beginAtZero: true, grid: { color: '#334155' }, ticks: { color: '#94a3b8' } },
                                x: { display: false }
                            },
                            plugins: { legend: { display: false } }
                        }
                    });
                }
                
                charts[pid].data.datasets[0].data = st.history;
                charts[pid].update();
            });
            
            Object.keys(charts).forEach(pid => {
                if (!data.pids.includes(pid)) {
                    charts[pid].destroy();
                    delete charts[pid];
                    const el = document.getElementById('chart-' + pid)?.parentElement?.parentElement;
                    if (el) el.remove();
                }
            });
            
            const logsContainer = document.getElementById('logs-container');
            logsContainer.innerHTML = data.logs.join('<br>');
            
            const oomTbody = document.getElementById('oom-mem-tbody');
            oomTbody.innerHTML = '';
            data.pids.forEach(pid => {
                const st = data.stats[pid];
                const maxMem = st.oom_max_mem || {};
                const oomHist = st.oom_mem_histogram || {};
                
                Object.keys(maxMem).sort().forEach(oom => {
                    const val = maxMem[oom];
                    const threshold = 0.95 * val;
                    const hist = oomHist[oom] || {};
                    let totalTime = 0;
                    let highTime = 0;
                    Object.keys(hist).forEach(bucketStr => {
                        const bucket = parseFloat(bucketStr);
                        const dur = hist[bucketStr];
                        totalTime += dur;
                        if (bucket >= threshold) highTime += dur;
                    });
                    
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${pid}</td>
                        <td>${getOomBadge(oom)} <span style="color:#94a3b8;font-size:0.9em">(${totalTime.toFixed(1)}s)</span></td>
                        <td>${val.toFixed(2)} MB <span style="color:#94a3b8;font-size:0.9em">(>${threshold.toFixed(1)} MB for ${highTime.toFixed(1)}s)</span></td>
                    `;
                    oomTbody.appendChild(tr);
                });
            });
        }
        
        async function saveSnapshot() {
            const data = await fetchData();
            let htmlContent = document.documentElement.outerHTML;
            const dataScript = `<script>window.STATIC_DATA = ${JSON.stringify(data)};</` + `script>`;
            htmlContent = htmlContent.replace('<head>', '<head>\\n    ' + dataScript);
            
            const blob = new Blob([htmlContent], { type: 'text/html' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'memory_monitor_snapshot.html';
            a.click();
            URL.revokeObjectURL(url);
        }
        
        if (!window.STATIC_DATA) {
            setInterval(updateDashboard, 1000);
        }
        updateDashboard();
    </script>
</body>
</html>"""


class DashboardHandler(BaseHTTPRequestHandler):
  """Handles HTTP requests for the local web dashboard.

  - GET /: Serves the main HTML interface string. - GET /api/data: Serves the
  latest memory stats, PID list, and logs as JSON.
  """

  def log_message(self, format, *args):
    # Suppress default HTTP request logging to keep the terminal output clean
    pass

  def do_GET(self):
    if self.path == "/":
      self.send_response(200)
      self.send_header("Content-type", "text/html; charset=utf-8")
      self.end_headers()
      self.wfile.write(HTML_CONTENT.encode("utf-8"))
    elif self.path == "/api/data":
      self.send_response(200)
      self.send_header("Content-type", "application/json")
      self.end_headers()

      with lock:
        clean_logs = [re.sub(r"\033\[.*?m", "", log) for log in logs[:200]]
        data = {"pids": pids, "stats": pid_stats, "logs": clean_logs}
      try:
        js_data = json.dumps(data)
        self.wfile.write(js_data.encode("utf-8"))
      except Exception as e:
        with open("json_error.log", "w") as f:
          f.write(str(e))
    else:
      self.send_response(404)
      self.end_headers()


def start_web_server():
  """Starts the local HTTP server in a daemon thread.

  Tries port 8080 first, and increments if the port is already in use.
  """
  global WEB_DASHBOARD_URL
  port = 8080
  while port < 8090:
    try:
      server = HTTPServer(("", port), DashboardHandler)
      WEB_DASHBOARD_URL = f"http://localhost:{port}"
      server.serve_forever()
      break
    except OSError:
      port += 1


if __name__ == "__main__":
  try:
    subprocess.check_output(["adb", "version"], stderr=subprocess.DEVNULL)
  except FileNotFoundError:
    print(
        "\033[1;31mError: 'adb' not found in PATH. Please ensure Android"
        " Platform Tools are installed and accessible.\033[0m"
    )
    sys.exit(1)

  print("Starting background threads...")
  threading.Thread(target=start_web_server, daemon=True).start()
  threading.Thread(target=update_pids_loop, daemon=True).start()
  threading.Thread(target=poll_oom_loop, daemon=True).start()
  threading.Thread(target=poll_meminfo_loop, daemon=True).start()

  try:
    while True:
      print_dashboard()
      time.sleep(0.1)
  except KeyboardInterrupt:
    print("\nSaving max memory summary to log file and exiting...")
    with lock:
      with open(LOG_FILENAME, "a") as f:
        f.write("\n" + "=" * 80 + "\n")
        f.write("MAX MEMORY SUMMARY\n")
        f.write("=" * 80 + "\n\n")
        for pid, st in pid_stats.items():
          name = st.get("name", "Unknown")
          f.write(f"PID: {pid} ({name})\n")
          f.write(
              f"{'OOM Score':<12} | {'Time Spent':<12} | Max Memory Observed\n"
          )
          f.write("-" * 80 + "\n")

          oom_max_mem = st.get("oom_max_mem", {})
          oom_mem_histogram = st.get("oom_mem_histogram", {})
          for oom in sorted(oom_max_mem.keys()):
            max_mem = oom_max_mem[oom]
            threshold = 0.95 * max_mem
            hist = oom_mem_histogram.get(oom, {})
            total_oom_time = sum(hist.values())
            time_spent = sum(dur for b, dur in hist.items() if b >= threshold)

            f.write(
                f"{oom:<12} | {total_oom_time:>8.1f}s | {max_mem:.2f} MB"
                f" (>{threshold:.1f} MB for {time_spent:.1f}s)\n"
            )
          f.write("\n")
    sys.exit(0)
