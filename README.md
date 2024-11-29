# CLI Reader and Programmer for RT0013 RFID Tags

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Description

This projects includes a Java-Library and a full cli-app for using RT0013 RFID Tags. It's ideal für enthusiasts and transport logistics.

## Features

- Guided setup, edit and reset of the tag
- Export of data inside tag
- Failsafe multi-tag handling (choose a tag)
- Tag buffering to RAM

## Requirements
Hardware:
- qLOG CAEN R1250l Tile Reader (or similar)
- qLOG CAEN RT0013 NFC Tag

Software:
- Java 17 or higher
- RFIDLibrary-5.0.0 (in /lib) ... CAEN API SDK 5.0.0
- jSerialComm-2.11.0 (in /lib) ... SDK dependency

## Installation (only Windows 10 tested)
- Install Java
- Make sure "java" is set in the Windows environment
- Download the release
- Install the included driver via Device-Manager for the unknown device
- Edit the COM-Port in "start.bat", take the "Virtual COM Port" (for example: "COM4") 
- Run / Double-click "start.bat" on Windows

## Dokumentation
- The code contains rudimentary function-descriptors. 
- A documentation is available in german. Please contact me if interested.

## ToDo / Known issues
- Date Conversion is pretty inaccurate... What's the mistake?
- Datastructures... I did my best to programm it in a modular way. 

## Licence
- CAEN did not explicitly allow the use of their SDK. It was downloaded with e-mail registration. Thus, this may not be for commercial use.  
