/**
 * Copyright (c) 2020-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.leastblurredimage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.StringJoiner;


public final class EventLogger extends EventListener{

	static{
		try{
			//check whether an optional SLF4J binding is available
			Class.forName("org.slf4j.impl.StaticLoggerBinder");
		}
		catch(final LinkageError | ClassNotFoundException ignored){
			System.out.println("[WARN] SLF4J: No logger is defined, NO LOG will be printed!");
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(EventLogger.class);


	private static class SingletonHelper{
		private static final EventLogger INSTANCE = new EventLogger();
	}


	public static EventLogger getInstance(){
		return SingletonHelper.INSTANCE;
	}

	EventLogger(){}

	@Override
	public void failedLoadingImage(final String image){
		error("Error opening image {}", image);
	}


	private void trace(final String message, final Exception exception){
		LOGGER.trace(composeMessage(message), exception);
	}

	private void trace(final String message, final Object... parameters){
		LOGGER.trace(composeMessage(message, parameters));
	}

	private void warn(final String message, final Object... parameters){
		LOGGER.warn(composeMessage(message, parameters));
	}

	private void info(final String message, final Object... parameters){
		LOGGER.info(composeMessage(message, parameters));
	}

	private void error(final String message, final Object... parameters){
		LOGGER.error(composeMessage(message, parameters));
	}

	private String composeMessage(final String message, final Object... parameters){
		final StringBuilder sb = new StringBuilder();
		try{
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			final Class<?> callerClass = Class.forName(stackTrace[3].getClassName());
			final int callerLineNumber = stackTrace[3].getLineNumber();

			sb.append('(')
				.append(callerClass.getSimpleName());
			if(callerLineNumber >= 0)
				sb.append(':')
					.append(callerLineNumber);
			sb.append(')')
				.append(' ');
		}
		catch(final ClassNotFoundException ignored){}

		if(message != null)
			sb.append(message);

		return JavaHelper.format(sb.toString(), extractParameters(parameters));
	}

	private Object[] extractParameters(final Object[] parameters){
		if(parameters instanceof Class<?>[]){
			final Collection<String> packages = new LinkedHashSet<>(parameters.length);
			for(final Object basePackageClass : parameters)
				packages.add(((Class<?>)basePackageClass).getPackageName());

			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final String p : packages)
				sj.add(p);

			return new Object[]{sj.toString()};
		}
		return parameters;
	}

}
