package de.fxdiagram.swtfx;

import static de.fxdiagram.swtfx.PrivateFieldAccessor.getPrivateField;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.sun.javafx.tk.TKSceneListener;

import javafx.application.Platform;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventType;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.ZoomEvent;

/**
 * A gesture listener that converts and transfers SWT {@link GestureEvent}s to an {@link FXCanvas}.
 *  
 * @author Jan Koehnlein
 */
public class SwtToFXGestureConverter implements GestureListener, MouseWheelListener {

	enum StateType {
		IDLE, SCROLLING, ROTATING, ZOOMING;
	}
	
	protected class State {
		StateType type;
		
		double totalScrollX = 0;
		double totalScrollY = 0;
		
		double lastZoomFactor = 1;
		double lastRotation = 0;
		
		public State(StateType type) {
			this.type = type;
		}
	}
	
	private Listener mouseWheelEmulatedEventFilter;

	private FXCanvas canvas;
	
	private State currentState;

	public SwtToFXGestureConverter(final FXCanvas canvas) {
		this.canvas = canvas;
		this.currentState = new State(StateType.IDLE);
		canvas.addGestureListener(this);
		canvas.addMouseWheelListener(this);
		Display display = canvas.getDisplay();
		if (display.getTouchEnabled()) {
			// register a filter to suppress emulated scroll events that
			// originate from mouse wheel events on devices that support touch
			// events (see #430940)
			mouseWheelEmulatedEventFilter = new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (event.widget == canvas) {
						event.type = SWT.None;
					}
				}
			};
			display.addFilter(SWT.MouseVerticalWheel,
					mouseWheelEmulatedEventFilter);
			display.addFilter(SWT.MouseHorizontalWheel,
					mouseWheelEmulatedEventFilter);
		}
	}	
	
	public void dispose() {
		if(!canvas.isDisposed()) {
			canvas.removeGestureListener(this);
			canvas.removeMouseWheelListener(this);
			canvas.dispose();
		}
		Display display = Display.getDefault();
		if (mouseWheelEmulatedEventFilter != null) {
			display.removeFilter(SWT.MouseVerticalWheel,
					mouseWheelEmulatedEventFilter);
			display.removeFilter(SWT.MouseHorizontalWheel,
					mouseWheelEmulatedEventFilter);
		}
	}

	@Override
	public void gesture(GestureEvent event) {
		sendGestureEventToFX(event);
	}

	@Override
	public void mouseScrolled(final MouseEvent e) {
		Platform.runLater(new Runnable() {
            @Override
            public void run() {
            	final Object scenePeer = getPrivateField(canvas, "scenePeer");
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                    	TKSceneListener sceneListener = getPrivateField(scenePeer, "sceneListener");
						if (sceneListener == null) {
                            return null;
                        }
						Event mockEvent = new Event();
						mockEvent.stateMask = e.stateMask;
						mockEvent.x = e.x;
						mockEvent.y = e.y;
						mockEvent.xDirection = 0;
						mockEvent.yDirection = e.count;
						mockEvent.widget = e.widget;
						mockEvent.display = e.display;
						mockEvent.time = e.time;
						mockEvent.data = e.data;
						GestureEvent mockGestureEvent = new GestureEvent(mockEvent);
						mockGestureEvent.stateMask = e.stateMask;
						sendScrollEvent(ScrollEvent.SCROLL_STARTED, mockGestureEvent, sceneListener);
						sendScrollEvent(ScrollEvent.SCROLL_FINISHED, mockGestureEvent, sceneListener);
                        return null;
                    }

                }, (AccessControlContext) getPrivateField(scenePeer, "accessCtrlCtx"));
            }
        });
	}
	
	protected void sendGestureEventToFX(final GestureEvent event) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
            	final Object scenePeer = getPrivateField(canvas, "scenePeer");
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                    	TKSceneListener sceneListener = getPrivateField(scenePeer, "sceneListener");
						if (sceneListener == null) {
                            return null;
                        }
						switch(event.detail) {
							case SWT.GESTURE_BEGIN:
								break;
							case SWT.GESTURE_END:
								changeState(StateType.IDLE, event, sceneListener);
								break;
							case SWT.GESTURE_MAGNIFY:
								changeState(StateType.ZOOMING, event, sceneListener);
								break;
							case SWT.GESTURE_PAN:
								changeState(StateType.SCROLLING, event, sceneListener);
								break;
							case SWT.GESTURE_ROTATE:
								changeState(StateType.ROTATING, event, sceneListener);
								break;
							case SWT.GESTURE_SWIPE:
								changeState(StateType.IDLE, event, sceneListener);
						}
                        return null;
                    }

                }, (AccessControlContext) getPrivateField(scenePeer, "accessCtrlCtx"));
            }
        });
	}
	
	protected boolean changeState(StateType newStateType, GestureEvent event, TKSceneListener sceneListener) {
		if(newStateType != currentState.type) { 
			switch (currentState.type) {
				case SCROLLING:
					sendScrollEvent(ScrollEvent.SCROLL_FINISHED, event, sceneListener);
					break;
				case ROTATING:
					sendRotateEvent(RotateEvent.ROTATION_FINISHED, event, sceneListener);
					break;
				case ZOOMING:
					sendZoomEvent(ZoomEvent.ZOOM_FINISHED, event, sceneListener);
					break;
				default:
					// do nothing
			}
			switch (newStateType) {
				case SCROLLING:
					sendScrollEvent(ScrollEvent.SCROLL_STARTED, event, sceneListener);
					break;
				case ROTATING:
					sendRotateEvent(RotateEvent.ROTATION_STARTED, event, sceneListener);
					break;
				case ZOOMING:
					sendZoomEvent(ZoomEvent.ZOOM_STARTED, event, sceneListener);
					break;
				case IDLE:
					if(event.detail == SWT.GESTURE_SWIPE)
						sendSwipeEvent(event, sceneListener);
					break;
				default:
					// do nothing
			}
			currentState = new State(newStateType);
			return true;
		}
		switch (newStateType) {
			case SCROLLING:
				sendScrollEvent(ScrollEvent.SCROLL, event, sceneListener);
				break;
			case ROTATING:
				sendRotateEvent(RotateEvent.ROTATE, event, sceneListener);
				break;
			case ZOOMING:
				sendZoomEvent(ZoomEvent.ZOOM, event, sceneListener);
				break;
			case IDLE:
				if(event.detail == SWT.GESTURE_SWIPE)
					sendSwipeEvent(event, sceneListener);
			default:
				// do nothing
		}
		return false;
	}

	private void sendScrollEvent(EventType<ScrollEvent> fxEventType,
			final GestureEvent event,
			TKSceneListener sceneListener) {
		currentState.totalScrollX += event.xDirection;
		currentState.totalScrollY += event.yDirection;
		Point screenPosition = canvas.toDisplay(event.x, event.y);
//		System.out.println(fxEventType + " " + screenPosition);
		sceneListener.scrollEvent(fxEventType, 
				event.xDirection, event.yDirection, // scrollX, scrollY
				0, 0,        // totalScrollX, totalScrollY
				-5.0, -5.0,  // xMultiplier, yMultiplier
				0,           // touchCount 
				0, 0,        // scrollTextX, scrollTextY
				0, 0,        // defaultTextX, defaultTextY
				event.x, event.y, // x, y
				screenPosition.x, screenPosition.y, // screenX, screenY
				isShift(event), isControl(event), isAlt(event), isMeta(event), 
				false,       // direct 
				false);      // inertia
	}
	
	private void sendZoomEvent(EventType<ZoomEvent> fxEventType,
			final GestureEvent event,
			TKSceneListener sceneListener) {
		Point screenPosition = canvas.toDisplay(event.x, event.y);
		double magnification = (fxEventType == ZoomEvent.ZOOM_FINISHED) 
				? currentState.lastZoomFactor
				: event.magnification;
//		System.out.println(fxEventType + " " + magnification);
		sceneListener.zoomEvent(fxEventType,
				magnification / currentState.lastZoomFactor, // zoom factor
				magnification,    // totalZoomFactor
				event.x, event.y,       // x, y
				screenPosition.x, screenPosition.y,           // screenX, screenY
				isShift(event), isControl(event), isAlt(event), isMeta(event), 
				false,      // direct 
				false);     // inertia
		currentState.lastZoomFactor = magnification;
	}
	
	private void sendRotateEvent(EventType<RotateEvent> fxEventType,
			final GestureEvent event,
			TKSceneListener sceneListener) {
		Point screenPosition = canvas.toDisplay(event.x, event.y);
		double rotation = (fxEventType == RotateEvent.ROTATION_FINISHED) 
				? currentState.lastRotation
				: -event.rotation;
//		System.out.println(fxEventType + " " + rotation);
		sceneListener.rotateEvent(fxEventType,
				rotation - currentState.lastRotation, // rotation
				rotation,               // totalRotation
				event.x, event.y,       // x, y
				screenPosition.x, screenPosition.y,           // screenX, screenY
				isShift(event), isControl(event), isAlt(event), isMeta(event), 
				false,      // direct 
				false);     // inertia
		currentState.lastRotation = rotation;
	}
	
	private void sendSwipeEvent(final GestureEvent event,
				TKSceneListener sceneListener) {
		Point screenPosition = canvas.toDisplay(event.x, event.y);
		EventType<SwipeEvent> fxEventType = null;
		if(event.yDirection > 0)
			fxEventType = SwipeEvent.SWIPE_DOWN;
		else if(event.yDirection < 0) 
			fxEventType = SwipeEvent.SWIPE_UP;
		else if(event.xDirection > 0)
			fxEventType = SwipeEvent.SWIPE_RIGHT;
		else if(event.xDirection < 0) 
			fxEventType = SwipeEvent.SWIPE_LEFT;
//		System.out.println(fxEventType.toString());
		sceneListener.swipeEvent(fxEventType,
				0,                  // touch
				event.x, event.y,   // x, y
				screenPosition.x, screenPosition.y, // screenX, screenY
				isShift(event), isControl(event), isAlt(event), isMeta(event), 
				false);             // direct 
	}
	
	private boolean isShift(final GestureEvent event) {
		return (event.stateMask & SWT.SHIFT) != 0;
	}

	private boolean isControl(final GestureEvent event) {
		return (event.stateMask & SWT.CONTROL) != 0;
	}

	private boolean isAlt(final GestureEvent event) {
		return (event.stateMask & SWT.ALT) != 0;
	}

	private boolean isMeta(final GestureEvent event) {
		return (event.stateMask & SWT.COMMAND) != 0;
	}
}
