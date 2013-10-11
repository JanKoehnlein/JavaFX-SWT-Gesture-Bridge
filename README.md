JavaFX-SWT-Gesture-Bridge
=========================

Transfers SWT multitouch gestures to embedded FX applications.

This functionality is missing in the standard FXCanvas implementation. 
See https://javafx-jira.kenai.com/browse/RT-33454

It works fine but can be considered a hack as we must access some private fielsd reflectively to hook into JavaFX.
