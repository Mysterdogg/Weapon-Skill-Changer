# Weapon Skill Changer by Mysterdogg

A small CLEO script for GTA San Andreas (US 1.0) that changes the skill level of the weapon the player is holding, or of every weapon at once. 
It is meant as a testing tool for anything related to weapon skills (dual wielding, accuracy, reload speed, moving while aiming...) and as a clean base for other mods.

## Requirements

- GTA San Andreas US 1.0
- CLEO 4.4.4

## Installation

1. Copy `WeaponSkillChanger.cs` & `WeaponSkillChanger.ini` into the game's `CLEO` folder.

## Controls

Hotkeys only work on foot. Numpad keys need NumLock ON.

| Key   | Action |
|-------|--------|
| Num 9 | Next tier for the current weapon (Poor > Gangster > Hitman > Poor) |
| Num 3 | Previous tier for the current weapon |
| Num 6 | Raise the raw skill value by `Step` (default 50) |
| Num 4 | Lower the raw skill value by `Step` |
| Num 5 | Show weapon, tier, raw value and thresholds |
| Num 7 | Set **all** weapon skills to 1000 (Hitman) |
| Num 1 | Reset **all** weapon skills to 0 (Poor) |

Note: this changes the real stats of your current game. If you save after using it, the changes are saved too. Use a test save or a backup copy.

## How it works

- Weapon skills are float stats from 69 to 79, with values from 0 to 1000. Weapons 22 to 31 map to stats 69 to 78 in the same order, Micro SMG and Tec-9 share stat 75, and Country Rifle and Sniper Rifle share stat 79. Only weapons 22 to 32 have tiers; the two rifles support raw values only.
- The Gangster and Hitman thresholds are read from the game's own weapon data at runtime, so the script follows a modded `weapon.dat` automatically.
- Selecting Gangster or Hitman sets the stat to the exact threshold, which is useful to test the boundary. Poor sets it to 0.
- Visual changes such as dual wielding show up instantly.

## Changelog

- **1.0.0**: First public version.
