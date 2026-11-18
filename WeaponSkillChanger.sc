{$CLEO .cs}
// ============================================================
// Weapon Skill Changer v1.0.0 by Mysterdogg
// GTA San Andreas US 1.0 | CLEO 4.4.4 | Sanny Builder 4.2.0
//
// Changes the skill level of the weapon the player is holding,
// or of all skill weapons at once. Meant as a testing tool and
// as a base for other mods.
//
// Default keys (on foot only, NumLock ON, editable in the INI):
//   Num 9 = next tier    (POOR -> GANGSTER -> HITMAN -> POOR...)
//   Num 3 = previous tier
//   Num 6 = raise raw skill value by Step (default 50)
//   Num 4 = lower raw skill value by Step
//   Num 5 = show info for the current weapon
//   Num 7 = set ALL weapon skills to 1000 (HITMAN)
//   Num 1 = reset ALL weapon skills to 0 (POOR)
//
// Weapon skill stats (IDs 69 to 79):
//   weapons 22..31 -> stats 69..78 (same order), 32 -> 75, 33 and 34 -> 79
// Tiers only exist for weapons 22..32. Country Rifle and Sniper
// Rifle (stat 79) only support raw values.
//
// GANGSTER and HITMAN thresholds are read from the game's weapon
// data at runtime, so a modded weapon.dat is followed automatically.
//
// WARNING: this changes the real stats of the current game. If you
// save after using it, the changes are saved too. Use a test save
// or a backup copy of your save.
//
// Optional config file: CLEO\WeaponSkillChanger.ini
// ============================================================

Alloc($PLAYER_ACTOR, 3)
Alloc($PLAYER_CHAR, 2)

// ---------------- Memory addresses (US 1.0) ----------------
const
  // CWeaponInfo* __cdecl CWeaponInfo::GetWeaponInfo(eWeaponType type, uchar skill)
  // skill: 0 = POOR, 1 = GANGSTER (std), 2 = HITMAN (pro)
  ADDR_CWeaponInfo_GetWeaponInfo = 0x743C60
  // int CWeaponInfo::m_nReqStatLevel (stat value required for that skill entry)
  OFFS_CWeaponInfo_ReqStatLevel = 0x34
end

// ---------------- Game constants ----------------
const
  WEAPON_PISTOL = 22
  WEAPON_TEC9 = 32
  WEAPON_COUNTRY_RIFLE = 33
  WEAPON_SNIPER = 34
  STAT_PISTOL_SKILL = 69
  STAT_MICRO_SMG_SKILL = 75   // shared by Micro SMG and Tec-9
  STAT_SNIPER_SKILL = 79      // shared by Country Rifle and Sniper Rifle
  STAT_OFFSET = 47            // weapons 22..31 -> stats 69..78
  SKILL_MAX = 1000
  NAME_STRIDE = 20            // bytes per entry in @Weapon_Names
  TIER_STRIDE = 12            // bytes per entry in @Tier_Names
end

// 0@..6@  = hotkeys: next tier, prev tier, raise, lower, info, max all, reset all
// 7@  = step              | 8@  = show feedback (0/1) | 9@  = modifier key (0 = none)
// 10@ = current weapon    | 11@ = skill stat ID (-1 = no skill)
// 12@ = stat value (float)| 13@ = stat value (int)
// 14@ = GANGSTER threshold| 15@ = HITMAN threshold    | 16@ = current tier (0..2)
// 17@ = temp              | 18@ = weapon name pointer | 19@ = tier name pointer
// 21@ = key down flag     | 22@ = weapon has tiers    | 23@ = key being checked
// 24@ = text time (ms)
var
  0@: Int
  1@: Int
  2@: Int
  3@: Int
  4@: Int
  5@: Int
  6@: Int
  7@: Int
  8@: Int
  9@: Int
  10@: Int
  11@: Int
  12@: Float
  13@: Int
  14@: Int
  15@: Int
  16@: Int
  17@: Int
  18@: Int
  19@: Int
  21@: Int
  22@: Int
  23@: Int
  24@: Int
end

// This line keeps :Main away from offset 0 of the script
// (Sanny Builder gives error 0084 if something jumps to offset 0).
03A4: name_thread "WSKILL"

// Defaults (Windows virtual-key codes)
0@ = 0x69   // Num 9
1@ = 0x63   // Num 3
2@ = 0x66   // Num 6
3@ = 0x64   // Num 4
4@ = 0x65   // Num 5
5@ = 0x67   // Num 7
6@ = 0x61   // Num 1
7@ = 50
8@ = 1
9@ = 0
24@ = 2000
gosub @Load_Ini

:Main
wait 0
if and
  is_player_playing $PLAYER_CHAR
  not is_char_in_any_car $PLAYER_ACTOR
goto_if_false @Main

// Optional modifier key must be held
if
  9@ > 0
goto_if_false @Key_Next
if
  0AB0: is_key_pressed 9@
goto_if_false @Main

:Key_Next
23@ = 0@
gosub @Is_Key_Down
if
  21@ == 1
goto_if_false @Key_Prev
gosub @Next_Tier
goto @Wait_Release

:Key_Prev
23@ = 1@
gosub @Is_Key_Down
if
  21@ == 1
goto_if_false @Key_Raise
gosub @Prev_Tier
goto @Wait_Release

:Key_Raise
23@ = 2@
gosub @Is_Key_Down
if
  21@ == 1
goto_if_false @Key_Lower
gosub @Raise_Value
goto @Wait_Release

:Key_Lower
23@ = 3@
gosub @Is_Key_Down
if
  21@ == 1
goto_if_false @Key_Info
gosub @Lower_Value
goto @Wait_Release

:Key_Info
23@ = 4@
gosub @Is_Key_Down
if
  21@ == 1
goto_if_false @Key_Max_All
gosub @Show_Info
goto @Wait_Release

:Key_Max_All
23@ = 5@
gosub @Is_Key_Down
if
  21@ == 1
goto_if_false @Key_Reset_All
13@ = SKILL_MAX
gosub @Set_All_Skills
if
  8@ == 1
goto_if_false @Wait_Release
print_string_now "ALL WEAPON SKILLS: ~g~HITMAN~w~ (1000)" {time} 24@
goto @Wait_Release

:Key_Reset_All
23@ = 6@
gosub @Is_Key_Down
if
  21@ == 1
goto_if_false @Main
13@ = 0
gosub @Set_All_Skills
if
  8@ == 1
goto_if_false @Wait_Release
print_string_now "ALL WEAPON SKILLS: ~r~POOR~w~ (0)" {time} 24@

// Wait until the key is released (avoids repeating the change)
:Wait_Release
wait 0
if
  0AB0: is_key_pressed 23@
goto_if_false @Main
goto @Wait_Release

// ---------------- Subroutine: is key down ----------------
// in: 23@ key code | out: 21@ = 1 if down (key 0 = disabled)
:Is_Key_Down
21@ = 0
if
  23@ > 0
goto_if_false @Is_Key_Down_End
if
  0AB0: is_key_pressed 23@
goto_if_false @Is_Key_Down_End
21@ = 1
:Is_Key_Down_End
return

// ---------------- Subroutines: actions ----------------
:Next_Tier
gosub @Read_Current
if
  11@ >= 0
goto_if_false @No_Skill
if
  22@ == 1
goto_if_false @No_Tiers
16@ += 1
if
  16@ > 2
goto_if_false @Next_Tier_Apply
16@ = 0
:Next_Tier_Apply
gosub @Apply_Tier
gosub @Feedback
return

:Prev_Tier
gosub @Read_Current
if
  11@ >= 0
goto_if_false @No_Skill
if
  22@ == 1
goto_if_false @No_Tiers
16@ -= 1
if
  0 > 16@
goto_if_false @Prev_Tier_Apply
16@ = 2
:Prev_Tier_Apply
gosub @Apply_Tier
gosub @Feedback
return

:Raise_Value
gosub @Read_Current
if
  11@ >= 0
goto_if_false @No_Skill
13@ += 7@
gosub @Write_Stat
gosub @Feedback
return

:Lower_Value
gosub @Read_Current
if
  11@ >= 0
goto_if_false @No_Skill
13@ -= 7@
gosub @Write_Stat
gosub @Feedback
return

// POOR = 0, GANGSTER and HITMAN = exact threshold from weapon data
:Apply_Tier
13@ = 0
if
  16@ == 1
goto_if_false @Apply_Tier_2
13@ = 14@
:Apply_Tier_2
if
  16@ == 2
goto_if_false @Apply_Tier_Write
13@ = 15@
:Apply_Tier_Write
gosub @Write_Stat
return

// ---------------- Subroutine: read current weapon ----------------
// out: 10@ weapon, 11@ stat ID (-1 if none), 12@/13@ value,
//      22@ has tiers, 14@/15@ thresholds, 16@ tier
:Read_Current
10@ = get_current_char_weapon $PLAYER_ACTOR
11@ = -1
22@ = 0
16@ = 0
if and
  10@ >= WEAPON_PISTOL
  WEAPON_SNIPER >= 10@
goto_if_false @Read_Current_End

// Weapons 22..31 -> stats 69..78
11@ = 10@
11@ += STAT_OFFSET
if
  10@ == WEAPON_TEC9
goto_if_false @Map_1
11@ = STAT_MICRO_SMG_SKILL
:Map_1
if
  10@ >= WEAPON_COUNTRY_RIFLE
goto_if_false @Map_2
11@ = STAT_SNIPER_SKILL
:Map_2
0653: get_float_stat 11@ store_to 12@
13@ =# 12@

// Country Rifle and Sniper Rifle have no tiers
if
  WEAPON_TEC9 >= 10@
goto_if_false @Read_Current_End
22@ = 1
gosub @Read_Thresholds
if
  13@ >= 14@
goto_if_false @Read_Current_End
16@ = 1
if
  13@ >= 15@
goto_if_false @Read_Current_End
16@ = 2
:Read_Current_End
return

// ---------------- Subroutine: read tier thresholds ----------------
// in: 10@ weapon | out: 14@ GANGSTER, 15@ HITMAN
// 0AA7 arguments are written last-to-first: (skill, weapon type)
:Read_Thresholds
0AA7: call_function ADDR_CWeaponInfo_GetWeaponInfo num_params 2 pop 2 1 10@ func_ret 17@
17@ += OFFS_CWeaponInfo_ReqStatLevel
0A8D: read_memory 17@ size 4 virtual_protect 0 store_to 14@
0AA7: call_function ADDR_CWeaponInfo_GetWeaponInfo num_params 2 pop 2 2 10@ func_ret 17@
17@ += OFFS_CWeaponInfo_ReqStatLevel
0A8D: read_memory 17@ size 4 virtual_protect 0 store_to 15@

// Sanity guards in case the weapon data is unusual
if and
  15@ > 0
  SKILL_MAX >= 15@
goto_if_false @Fix_Hitman
goto @Check_Gangster
:Fix_Hitman
15@ = SKILL_MAX
:Check_Gangster
if and
  14@ >= 0
  15@ >= 14@
goto_if_false @Fix_Gangster
return
:Fix_Gangster
14@ = 15@
return

// ---------------- Subroutine: write stat ----------------
// in: 11@ stat ID, 13@ value (clamped to 0..1000)
:Write_Stat
if
  0 > 13@
goto_if_false @Write_Stat_Max
13@ = 0
:Write_Stat_Max
if
  13@ > SKILL_MAX
goto_if_false @Write_Stat_Set
13@ = SKILL_MAX
:Write_Stat_Set
12@ =# 13@
062A: set_float_stat 11@ to 12@
return

// in: 13@ value | sets stats 69..79 (all skill weapons)
:Set_All_Skills
11@ = STAT_PISTOL_SKILL
:Set_All_Loop
gosub @Write_Stat
11@ += 1
if
  STAT_SNIPER_SKILL >= 11@
goto_if_false @Set_All_End
goto @Set_All_Loop
:Set_All_End
return

// ---------------- Subroutines: on-screen text ----------------
:Feedback
if
  8@ == 1
goto_if_false @Feedback_End
gosub @Show_Info
:Feedback_End
return

:Show_Info
gosub @Read_Current
if
  11@ >= 0
goto_if_false @No_Skill

// Weapon name pointer = @Weapon_Names + (weapon - 22) * NAME_STRIDE
0AC6: get_label_pointer @Weapon_Names store_to 18@
17@ = 10@
17@ -= WEAPON_PISTOL
17@ *= NAME_STRIDE
18@ += 17@

if
  22@ == 1
goto_if_false @Show_Info_Raw

// Tier name pointer = @Tier_Names + tier * TIER_STRIDE
0AC6: get_label_pointer @Tier_Names store_to 19@
17@ = 16@
17@ *= TIER_STRIDE
19@ += 17@
0AD1: print_formatted_now "~y~%s~w~ - SKILL: ~g~%s~w~ (%d/1000)~n~GANGSTER at %d, HITMAN at %d" {time} 24@ 18@ 19@ 13@ 14@ 15@
return

:Show_Info_Raw
0AD1: print_formatted_now "~y~%s~w~ - SKILL: %d/1000 (no tiers)" {time} 24@ 18@ 13@
return

:No_Skill
print_string_now "THIS WEAPON HAS NO SKILL LEVEL" {time} 24@
return

:No_Tiers
print_string_now "THIS WEAPON HAS NO TIERS (RAW VALUE ONLY)" {time} 24@
return

// ---------------- Subroutine: load config ----------------
// Any missing key keeps its default value.
:Load_Ini
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "NextTier" store_to 17@
goto_if_false @Ini_1
0@ = 17@
:Ini_1
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "PrevTier" store_to 17@
goto_if_false @Ini_2
1@ = 17@
:Ini_2
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "RaiseValue" store_to 17@
goto_if_false @Ini_3
2@ = 17@
:Ini_3
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "LowerValue" store_to 17@
goto_if_false @Ini_4
3@ = 17@
:Ini_4
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "ShowInfo" store_to 17@
goto_if_false @Ini_5
4@ = 17@
:Ini_5
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "MaxAll" store_to 17@
goto_if_false @Ini_6
5@ = 17@
:Ini_6
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "ResetAll" store_to 17@
goto_if_false @Ini_7
6@ = 17@
:Ini_7
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Keys" "Modifier" store_to 17@
goto_if_false @Ini_8
9@ = 17@
:Ini_8
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Settings" "Step" store_to 17@
goto_if_false @Ini_9
if
  17@ > 0
goto_if_false @Ini_9
7@ = 17@
:Ini_9
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Settings" "ShowFeedback" store_to 17@
goto_if_false @Ini_10
8@ = 17@
:Ini_10
if
  0AF0: read_int_from_ini_file "cleo\WeaponSkillChanger.ini" "Settings" "TextTime" store_to 17@
goto_if_false @Ini_End
if
  17@ > 0
goto_if_false @Ini_End
24@ = 17@
:Ini_End
return

// ---------------- Data (never executed) ----------------
// Weapons 22..34, NAME_STRIDE (20) bytes each, zero-padded
:Weapon_Names
hex
  "Pistol" 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  "Silenced Pistol" 00 00 00 00 00
  "Desert Eagle" 00 00 00 00 00 00 00 00
  "Shotgun" 00 00 00 00 00 00 00 00 00 00 00 00 00
  "Sawn-off Shotgun" 00 00 00 00
  "Combat Shotgun" 00 00 00 00 00 00
  "Micro SMG" 00 00 00 00 00 00 00 00 00 00 00
  "SMG" 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  "AK-47" 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  "M4" 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  "Tec-9" 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  "Country Rifle" 00 00 00 00 00 00 00
  "Sniper Rifle" 00 00 00 00 00 00 00 00
end

// Tiers 0..2, TIER_STRIDE (12) bytes each, zero-padded
:Tier_Names
hex
  "POOR" 00 00 00 00 00 00 00 00
  "GANGSTER" 00 00 00 00
  "HITMAN" 00 00 00 00 00 00
end
