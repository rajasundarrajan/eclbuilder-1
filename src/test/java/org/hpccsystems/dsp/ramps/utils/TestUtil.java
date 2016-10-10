package org.hpccsystems.dsp.ramps.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.InvocationTargetException;

/**
 * @See {@link TestUtil#validateException(Object, String, Class[], Class, String, Throwable, Object...)} 
 * @author Ashoka_K
 *
 */
public class TestUtil {
	
	/**
	 * Convenience method for verifying exceptions thrown by the given constructor
	 * See {@link TestUtil#validateException(Object, String, Class[], Class, String, Throwable, Object...)}
	 * 
	 * @param testClass The class for which the constructor is to be invoked
	 * @param exceptionClass The class of the exception to be expected
	 * @param message The expected message in the expected exception
	 * @param params An object array which contains the arguments for the constructor
	 * to be invoked
	 * 
	 * @throws Exception In case an exception occurs when verifying the exceptions
	 */
	public static void validateInstantiationException(Class<?> testClass, Class<?> exceptionClass,
			String message, Object... params) throws Exception {
		validateException(testClass, ((String) null), null, exceptionClass, message, null, params);
	}
	
	/**
	 * Convenience method for verifying exceptions thrown by the given constructor
	 * {@link TestUtil#validateException(Object, String, Class[], Class, String, Throwable, Object...)}
	 * 
	 * @param testClass The class for which the constructor is to be invoked
	 * @param exceptionClass The class of the exception to be expected
	 * @param message The expected message in the expected exception
	 * @param cause The cause to be expected within the expected exception
	 * @param params An object array which contains the arguments for the constructor
	 * 
	 * @throws Exception In case an exception occurs when verifying the exceptions
	 */
	public static void validateInstantiationException(Class<?> testClass,  Class<?> exceptionClass,
			String message, Throwable cause, Object... params) throws Exception {
		validateException(testClass, ((String) null), null, exceptionClass, message, cause, params);
	}
	
	/**
	 * Convenience method for verifying exceptions thrown by the given constructor
	 * {@link TestUtil#validateException(Object, String, Class[], Class, String, Throwable, Object...)}
	 *
	 * @param testClass The class for which the constructor is to be invoked
	 * @param paramTypes The parameter types for the method/constructor to be invoked.
	 * @param exceptionClass The class of the exception to be expected
	 * @param message The expected message in the expected exception
	 * @param cause The cause to be expected within the expected exception
	 * @param params An object array which contains the arguments for the constructor
	 * 
	 * @throws Exception In case an exception occurs when verifying the exceptions
	 */
	public static void validateInstantiationException(Class<?> testClass,  Class<?>[] paramTypes, Class<?> exceptionClass,
			String message, Throwable cause, Object... params) throws Exception {
		validateException(testClass, ((String) null), paramTypes, exceptionClass, message, cause, params);
	}
	
	/**
	 * Convenience method for verifying exceptions thrown by the given method
	 * {@link TestUtil#validateException(Object, String, Class[], Class, String, Throwable, Object...)}
	 * 
	 * @param obj The object on which the method (<code>methodName</code>) is to be invoked.
	 * @param methodName The name of the method to be invoked.
	 * @param exceptionClass The class of the exception to be expected
	 * @param message The expected message in the expected exception
	 * @param params An object array which contains the arguments for the constructor
	 * @throws Exception In case an exception occurs when verifying the exceptions
	 */
	public static void validateException(Object obj, String methodName, Class<?> exceptionClass,
			String message, Object... params) throws Exception {
		Class<? extends Object>[] paramTypes = getParameterTypes(params);
		validateException(obj, methodName, paramTypes, exceptionClass, message, null, params);
	}
	
	/**
	 * Convenience method for verifying exceptions thrown by the given method
	 * {@link TestUtil#validateException(Object, String, Class[], Class, String, Throwable, Object...)}
	 * 
	 * @param obj The object on which the method (<code>methodName</code>) is to be invoked.
	 * @param methodName The name of the method to be invoked.
	 * @param exceptionClass The class of the exception to be expected
	 * @param message The expected message in the expected exception
	 * @param cause The cause to be expected within the expected exception
	 * @param params An object array which contains the arguments for the constructor
	 * 
	 * @throws Exception In case an exception occurs when verifying the exceptions
	 */
	public static void validateException(Object obj, String methodName, Class<?> exceptionClass,
			String message, Throwable cause, Object... params) throws Exception {
		Class<? extends Object>[] paramTypes = getParameterTypes(params);
		validateException(obj, methodName, paramTypes, exceptionClass, message, null, params);
	}
	
	/**
	 * A convenience method which verifies the correctness of the exceptions thrown for the specified
	 * scenarios. See the description of the params for more details.
	 * 
	 * @param obj The object on which the method (<code>methodName</code>) is to be invoked. Will be the relevant
	 * Class object in case of constructor invocation.
	 * @param methodName The name of the method to be invoked. To be passed in as null in case of constructor
	 * invocation
	 * @param paramTypes The parameter types for the method/constructor to be invoked. Needs to be passed in only
	 * in case the paramTypes cannot be determined from the <code>params</code>, e.g. in case any of the objects
	 * within the <code>params</code> is null, or in case an object being passed in not the same class (e.g. subclass)
	 * of the type declared in the method signature.
	 * @param exceptionClass The class of the exception expected from the method (<code>methodName</code>) invocation.
	 * @param message The expected message in the expected exception
	 * @param cause The cause (if applicable) to be expected within the expected exception
	 * @param params An object array which contains the arguments for the method/constructor to be invoked
	 * 
	 * @throws Exception
	 */
	public static void validateException(Object obj, String methodName, Class<?>[] paramTypes, Class<?> exceptionClass,
			String message, Throwable cause, Object... params) throws Exception {
		if(params == null) {
			params = new Object[0];
		}
		
		Class<? extends Object> objClass = (methodName != null)? obj.getClass(): (Class<?>) obj;
		Exception exception = null;
		
		try {
			if(methodName != null) {
				objClass.getDeclaredMethod(methodName, paramTypes).invoke(obj, params);
			} else {
				objClass.getDeclaredConstructor(paramTypes).newInstance(params);
			}
		} catch(InvocationTargetException e) {
			exception = (Exception) e.getCause();
		}
		
		assertNotNull(exception);
		assertEquals(exceptionClass, exception.getClass());
		assertEquals(message, exception.getMessage());
		
		if(cause != null) {
			assertSame(cause, exception.getCause());
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<?>[] getParameterTypes(Object[] params) {
		if(params == null) {
			return new Class[0];
		}
		
		Class<? extends Object>[] paramTypes = new Class[params.length];
		int i = 0;
		
		for(Object param: params) {
			paramTypes[i++] = param.getClass();
		}
		
		return paramTypes;
	}
	
}
