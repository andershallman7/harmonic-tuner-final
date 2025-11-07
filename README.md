# Harmonic Tuner - Quick Start Guide

## Windows Users

You can run the tuner in two easy ways:

### Option 1: Batch File (Recommended - Double-click to run!)
1. Double-click **Run Tuner.bat**
2. Wait for compilation to finish
3. The tuner window will open

### Option 2: PowerShell Script (For advanced users)
1. Right-click on **Run Tuner.ps1**
2. Select "Run with PowerShell"
3. Follow the on-screen instructions

## First-Time Setup

The tuner will automatically:
1. Check for Java on your system
2. Compile the Java source files
3. Launch the tuner application

If Java is not installed, you'll see an error message with a link to download it.

## How to Use the Tuner

1. **Select your microphone** from the dropdown
2. (Optional) **Change the target frequency** (default is 440 Hz = A4)
3. Click **Start** to begin tuning
4. **Play a note** on your instrument
5. Watch the display to see:
   - The note name (e.g., "A4", "C#3")
   - The detected frequency in Hz
   - How many cents sharp/flat you are
   - A slider showing if you're sharp or flat
6. **Adjust your instrument** until the cents are close to 0

## Troubleshooting

- **No microphone detected?**: Check your system's audio input settings
- **Java not found?**: Install Java 8 or higher from [oracle.com](https://www.oracle.com/java/technologies/javase-downloads.html)
- **Permission errors?**: Run the launcher as administrator
- **Compilation errors?**: Make sure all files are in the correct locations

## What You Need

- Windows 7 or newer
- Java 8 or higher
- A working microphone

## Features

- Real-time pitch detection
- Musical note display
- Cents deviation from target
- Visual tuning indicator
- Multiple microphone support
- Adjustable reference pitch

Enjoy tuning your instruments!