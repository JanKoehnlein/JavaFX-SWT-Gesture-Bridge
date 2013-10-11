JavaFX-SWT-Gesture-Bridge
=========================

Transfers SWT multitouch gestures to embedded FX applications.

This functionality is missing in the standard FXCanvas implementation. 
See https://javafx-jira.kenai.com/browse/RT-33454

Known limitations:
- SWT detects vertial scrolls and passes them on as mouse wheel events. JavaFX already converts the latter to ScrollEvents with a very coarse granularity of 40. This is why veritcal scrolling looks bad compared to horizontal scrolling.
- It works fine but can be considered a hack as we must access some private field reflectively to hook into JavaFX.
