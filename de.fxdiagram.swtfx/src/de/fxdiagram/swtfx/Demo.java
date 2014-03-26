package de.fxdiagram.swtfx;

import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.VBox;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Demo {

	private static Scene createScene() {
		VBox root = new VBox();
		root.setAlignment(Pos.CENTER);
		root.getChildren().add(new Label("Try some multitouch gestures"));
		final Label display = new Label();
		root.getChildren().add(display);
		root.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				display.setText(event.getEventType().toString());
			}
		});
		root.addEventHandler(ZoomEvent.ANY, new EventHandler<ZoomEvent>() {
			@Override
			public void handle(ZoomEvent event) {
				display.setText(event.getEventType().toString());
			}
		});
		root.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
			@Override
			public void handle(RotateEvent event) {
				display.setText(event.getEventType().toString());
			}
		});
		root.addEventHandler(SwipeEvent.ANY, new EventHandler<SwipeEvent>() {
			@Override
			public void handle(SwipeEvent event) {
				display.setText(event.getEventType().toString());
			}
		});
		return new Scene(root, 300, 300);
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		FXCanvas canvas = new FXCanvas(shell, SWT.NONE);
		SwtToFXGestureConverter gestureConverter = new SwtToFXGestureConverter(canvas);
		Scene scene = createScene();
		canvas.setScene(scene);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		gestureConverter.dispose();
		display.dispose();
		
	}
}
