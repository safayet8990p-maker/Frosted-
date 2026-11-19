# Glassly — Liquid Glass Coverage Audit (`v0.01.10`)

Every floating control, bar, button, menu, sheet, dialog, switch, slider, stepper, chip, badge, and text field across Glassly uses the shared Liquid Glass component library (`com.example.glass`). Photo/video content surfaces stay un-glassed, and bottom sheets use flat tonal rows (`GlassFlatSheetRow`) inside a single outer glass shell to strictly avoid glass-inside-glass.

| Screen / Surface | Control / Element | Component Used | Glass Variant / Rule | Status |
|---|---|---|---|---|
| **Splash & Onboarding** | Skip Tour, Continue, Allow Access, Get Started | `GlassButton` | `CLEAR` / `PROMINENT` | PASS |
| **Splash & Onboarding** | Slide & Permission Cards | `GlassCard` | `REGULAR` over live Aurora backdrop | PASS |
| **Global Navigation (Handheld)** | Bottom-Left `[Library \| Collections]` Capsule & `(Search)` Circle | `GlassSplitBottomBar` + `GlassContainer` | `REGULAR` + SDF droplet morph + shrink on scroll | PASS |
| **Global Navigation (Selection)** | Bottom Bar `[Share \| X Selected \| Delete]` | `GlassSplitBottomBar` | `REGULAR` + destructive Trash tint | PASS |
| **Library Screen** | Time Segment Toggle, Filter/Tune, Select, Batch `...`, Cancel | `GlassIconButton` / `GlassButton` | `REGULAR` / `TINTED` | PASS |
| **Library Screen** | Years / Months / All Bar Switcher | `GlassSegmentedControl` | `REGULAR` + `TINTED` sliding indicator | PASS |
| **Library Screen** | Filter / Sort / View Options & Batch Menus | `GlassPopoverMenu` + `GlassMenuRow` | `REGULAR` + `thicknessBoost` expanding from trigger | PASS |
| **Library Screen** | Media Badges (Video Duration, Fav Heart, RAW, GIF, Motion, SCR) | `GlassBadge` | `CLEAR` + `adaptiveDarkBackdrop` scrim | PASS |
| **Library Screen** | Fast Scroll Date Scrubber | `GlassScrubberBubble` | `TINTED` | PASS |
| **Library Screen** | Delete Confirmation | `GlassAlertDialog` | `REGULAR` + `thicknessBoost` expanding from Trash | PASS |
| **Collections Screen** | `...` Menu, `PG` Profile/Settings Circle, Reorder Done, Up/Down Arrows | `GlassIconButton` / `GlassButton` | `REGULAR` / `TINTED` / `PROMINENT` | PASS |
| **Collections Screen** | Layout Switcher (3-icon row) & Options Menu | `GlassPopoverMenu` | `REGULAR` + `thicknessBoost` | PASS |
| **Collections Screen** | Pinned Sheet, Pin/Unpin Context, Create/Rename Album Sheets | `GlassBottomSheetOverlay` + `GlassFlatSheetRow` | Outer `REGULAR` glass; flat tonal rows inside | PASS |
| **Collections Screen** | Bottom `Reorder` Pill, Media Types & Utilities Group Cards | `GlassButton` / `GlassCard` | `TINTED` / `REGULAR` | PASS |
| **Search Overlay** | Bottom Thumb-Reach Search Field | `GlassSearchField` | `REGULAR` | PASS |
| **Search Overlay** | Close, Suggestion Chips, Result Tabs (`Photos \| Collections`), Cards | `GlassIconButton`, `GlassChip`, `GlassSegmentedControl`, `GlassCard` | `REGULAR` / `PROMINENT` | PASS |
| **Photo & Video Viewer** | Top Bar (`Back`, Date/Place Title Pill, `More ...`), Spatial Hexagon | `GlassIconButton`, `liquidGlass` Title Pill | `REGULAR` / `TINTED` (Blue when Spatial active) | PASS |
| **Photo & Video Viewer** | Active `SPATIAL SCENE` Top Pill, Motion `LIVE` Badge | `GlassButton`, `GlassBadge` | `PROMINENT` / `CLEAR` scrim | PASS |
| **Photo & Video Viewer** | Bottom Action Bar (`Share`, `Fav`, `Info`, `Edit`, `Delete`) | `liquidGlass` Capsule + `GlassIconButton` | `REGULAR` / `CLEAR` | PASS |
| **Photo & Video Viewer** | Video Transport (`-10s`, Play/Pause, `+10s`), Scrubber, Speed Popover | `GlassIconButton`, `GlassSlider`, `GlassPopoverMenu` | `REGULAR` / `TINTED` | PASS |
| **Photo & Video Viewer** | Info & EXIF Panel, Delete Confirm, More Actions Menu | `GlassBottomSheetOverlay`, `GlassAlertDialog`, `GlassPopoverMenu` | Outer `REGULAR` glass; flat EXIF rows | PASS |
| **Photo Editor** | Top Bar (`Cancel`, `Undo`, `Redo`, `Markup`, `More ...`, Yellow `Done`) | `GlassButton`, `GlassIconButton` | `REGULAR` / `PROMINENT` | PASS |
| **Photo Editor** | `...` Menu (Copy/Paste/Duplicate/Appearance) & Markup `+` Menu | `GlassPopoverMenu` + `GlassMenuRow` | `REGULAR` + `thicknessBoost` | PASS |
| **Photo Editor** | Tool Tabs, Adjust/Filter/Crop Chips, Adjust & Straighten Sliders | `GlassSegmentedControl`, `GlassChip`, `GlassSlider` | `REGULAR` / `PROMINENT` | PASS |
| **Photo Editor (Loupe)** | Floating Loupe Mini-Toolbar (Stroke Colors, `pt` Stepper, Duplicate, Delete) | `liquidGlass` Pill + `GlassStepper` + `GlassIconButton` | `REGULAR` | PASS |
| **Photo Editor** | Save as Copy / Overwrite & Add Text Sheets | `GlassBottomSheetOverlay` + `GlassFlatSheetRow` | Outer `REGULAR` glass; flat rows inside | PASS |
| **Trash / Hidden / Cleaners / Slideshow** | Empty Trash Confirm, PIN Keypad + Biometrics, Restore/Delete, Slideshow Bar | `GlassAlertDialog`, `GlassButton`, `GlassIconButton`, `GlassBadge` | `REGULAR` / `TINTED` / `CLEAR` | PASS |
| **Settings** | Collapsing Header, Back, Live Preview Card, All Section Cards | `GlassIconButton`, `GlassCard` | `REGULAR` over Aurora backdrop | PASS |
| **Settings** | Theme, Glass Style, Language Segmented Controls; Sliders; Steppers; Switches | `GlassSegmentedControl`, `GlassSlider`, `GlassStepper`, `GlassSwitch` | `REGULAR` / `TINTED` / `CLEAR` | PASS |
| **Settings** | Reset All Settings Alert, Vault PIN & Licenses Alerts | `GlassAlertDialog` | `REGULAR` + `thicknessBoost` | PASS |
| **Debug Showcase** | All Liquid Glass Variants, States, Droplet Container Demo | `GlassGalleryDebugScreen` | All variants & disabled/pressed states | PASS |
