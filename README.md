# Chesstastic

## Overview
**Chesstastic** is an Android application designed to play Chess offline using **Meshtastic** radios for connectivity. It combines a standard Chess interface with a mesh network backend, allowing players to communicate moves over long distances without cellular or internet capability.

## Features
- **Chess Board UI**: Interactive board built with Jetpack Compose.
- **Rule Enforcement**: Validates standard legal moves (Move geometry, Check detection).
- **Mesh Connectivity**: Sends and receives moves via Meshtastic text broadcasts.

## Move Notation
The app uses a simplified **Long Algebraic Notation** for all moves.
- **Format**: `[SourceSquare][TargetSquare]`
- **Examples**:
  - `e2e4` (Pawn moves from e2 to e4)
  - `g1f3` (Knight moves from g1 to f3)

## Meshtastic Protocol
Moves are transmitted as plain text messages over the mesh network.
- **Payload**: `CHESS:[MoveString]`
- **Example**: `CHESS:e2e4`

## Getting Started
1. Install the app on an Android device.
2. Ensure the Meshtastic app is installed and configured.
3. Use the interface to play. Moves are automatically sent via the Mesh integration (simulated in MVP via manual input).
