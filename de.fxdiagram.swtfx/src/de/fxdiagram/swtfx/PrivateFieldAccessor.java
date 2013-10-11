package de.fxdiagram.swtfx;

import java.lang.reflect.Field;

/**
 * Hack: Allows to access the value of private fields.
 * 
 * @author Jan Koehnlein
 */
public class PrivateFieldAccessor {
	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField(Object owner, String fieldName) {
		Class<? extends Object> currentClass = owner.getClass();
		Field field = null;
		do {
			try {
				field = currentClass.getDeclaredField(fieldName);
			} catch(NoSuchFieldException e) {
				currentClass = currentClass.getSuperclass();
				if(currentClass == null)
					return null;
			}
		} while(field == null);
		field.setAccessible(true);
		try {
			return (T) field.get(owner);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
