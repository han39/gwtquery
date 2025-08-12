/*
 * Copyright 2011, The gwtquery team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.query.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.query.client.js.JsUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

/**
 * Extend this class to implement functions callbacks.
 */
public abstract class Function {

	private com.google.gwt.dom.client.Element element = null;
	private Event event = null;
	private int index = -1;
	protected Object[] arguments = new Object[0];

	/**
	 * Override this for GQuery methods which take a callback and do not expect a return value.
	 *
	 * @param elem
	 *           takes a com.google.gwt.user.client.Element
	 */
	private boolean loop = false;

	/**
	 * Convenience alias for the getArguments(idx) method thought just to make gQuery code look closed to jQuery.
	 */
	public <T> T arguments(final int idx) {
		return getArgument(idx);
	}

	/**
	 * Convenience alias for the getArguments(argIdx, pos) method.
	 */
	public <T> T arguments(final int argIdx, final int pos) {
		return getArgument(argIdx, pos);
	}

	/**
	 * Override this for methods which invoke a cancel action.
	 *
	 * @param e
	 *           takes a com.google.gwt.dom.client.Element.
	 *
	 */
	public void cancel(final com.google.gwt.dom.client.Element e) {
		setElement(e);
	}

	/**
	 * Override this to define a function which does not need any parameter.
	 */
	public void f() {
		throw new RuntimeException("You have to override the adequate method to handle " +
				"this action, or you have to override 'public void f()' to avoid this error");
	}

	/**
	 * Override this for GQuery methods which take a callback and do not expect a return value.
	 *
	 * @param e
	 *           takes a com.google.gwt.dom.client.Element
	 */
	public void f(final com.google.gwt.dom.client.Element e) {
		setElement(e);
		final Widget w = e != null ? GQuery.getAssociatedWidget(e) : null;
		if (w != null) {
			loop = true;
			f(w);
		} else {
			f();
		}
	}

	/**
	 * Override this for GQuery methods which loop over matched elements and invoke a callback on each element.
	 *
	 * @param e
	 *           takes a com.google.gwt.dom.client.Element.
	 *
	 */
	public Object f(final com.google.gwt.dom.client.Element e, final int i) {
		setElement(e);
		setIndex(i);
		final Widget w = GQuery.getAssociatedWidget(e);
		if (w != null) {
			f(w, i);
		} else {
			f(e.<com.google.gwt.dom.client.Element> cast());
		}
		return null;
	}

	/**
	 * Override this method for bound event handlers.
	 *
	 * @return boolean false means stop propagation and prevent default
	 */
	public boolean f(final Event e) {
		setEvent(e);
		f(element);
		return true;
	}

	/**
	 * Does this fix the issue with gwt????????????????????
	 *
	 * Override this method for bound event handlers if you wish to deal with per-handler user data.
	 *
	 * @return boolean false means stop propagation and prevent default
	 */
	public boolean f(final Event e, final Object arg) {
		setArguments(arg);
		setEvent(e);
		return f(e);
	}

	/**
	 * Override this method for bound event handlers if you wish to deal with per-handler user data.
	 *
	 * @return boolean false means stop propagation and prevent default
	 */
	public boolean f(final Event e, final Object... arg) {
		setArguments(arg);
		setEvent(e);
		return f(e);
	}

	/**
	 * Override this method for bound callbacks.
	 */
	public void f(final int i, final Object arg) {
		f(i, new Object[] { arg });
	}

	/**
	 * Override this method for bound callbacks.
	 */
	public void f(final int i, final Object... args) {
		setIndex(i);
		setArguments(args);
		if (args.length == 1 && args[0] instanceof JavaScriptObject) {
			if (JsUtils.isElement(args[0])) {
				setElement((com.google.gwt.dom.client.Element) args[0]);
				f(getElement(), i);
			} else if (JsUtils.isEvent((JavaScriptObject) args[0])) {
				setEvent((Event) args[0]);
				f(getEvent());
			} else {
				f();
			}
		}
	}

	/**
	 * Override this method for bound callbacks.
	 */
	public Object f(final Object... args) {
		setArguments(args);
		f();
		return true;
	}

	/**
	 * Override this for GQuery methods which take a callback, but do not expect a return value, apply to a single widget.
	 *
	 * NOTE: If your query has non-widget elements you might need to override 'public void f()' or 'public void f(Element e)' to handle these elements and avoid a runtime exception.
	 */
	public void f(final Widget w) {
		setElement(w.getElement());
		if (loop) {
			loop = false;
			f();
		} else {
			f(w.getElement().<com.google.gwt.dom.client.Element> cast());
		}
	}

	/**
	 * Override this for GQuery methods which loop over matched widgets and invoke a callback on each widget.
	 *
	 * NOTE: If your query has non-widget elements you might need to override 'public void f()' or 'public void f(Element e)' to handle these elements and avoid a runtime exception.
	 */
	public Object f(final Widget w, final int i) {
		setElement(w.getElement());
		setIndex(i);
		f(w);
		return null;
	}

	/**
	 * Methods fe(...) should be used from asynchronous contexts so as we can catch the exception and send it to the GWT UncaughtExceptionHandler. They are intentionally final to avoid override them
	 */
	public final void fe() {
		fe(arguments);
	}

	/**
	 * Methods fe(...) should be used from asynchronous contexts so as we can catch the exception and send it to the GWT UncaughtExceptionHandler They are intentionally final to avoid override them.
	 */
	public final void fe(final com.google.gwt.dom.client.Element elem) {
		if (GWT.getUncaughtExceptionHandler() != null) {
			try {
				f(elem.<com.google.gwt.dom.client.Element> cast());
			} catch (final Exception e) {
				GWT.getUncaughtExceptionHandler().onUncaughtException(e);
			}
			return;
		}
		f(elem.<com.google.gwt.dom.client.Element> cast());
	}

	/**
	 * Methods fe(...) should be used from asynchronous contexts so as we can catch the exception and send it to the GWT UncaughtExceptionHandler They are intentionally final to avoid override them.
	 */
	public final boolean fe(final Event ev, final Object... args) {
		if (GWT.getUncaughtExceptionHandler() != null) {
			try {
				return f(ev, args);
			} catch (final Exception e) {
				GWT.getUncaughtExceptionHandler().onUncaughtException(e);
			}
			return true;
		}
		return f(ev, args);
	}

	/**
	 * Methods fe(...) should be used from asynchronous contexts so as we can catch the exception and send it to the GWT UncaughtExceptionHandler They are intentionally final to avoid override them.
	 */
	public final void fe(final Object arg) {
		fe(new Object[] { arg });
	}

	/**
	 * Methods fe(...) should be used from asynchronous contexts so as we can catch the exception and send it to the GWT UncaughtExceptionHandler They are intentionally final to avoid override them.
	 */
	public final Object fe(final Object... args) {
		if (GWT.getUncaughtExceptionHandler() != null) {
			try {
				return f(args);
			} catch (final Exception e) {
				GWT.getUncaughtExceptionHandler().onUncaughtException(e);
			}
			return true;
		}
		return f(args);
	}

	/**
	 * Return the argument in the position idx or null if it doesn't exist.
	 *
	 * Note: if the return type doesn't match the object, you will get a casting exception.
	 */
	public <T> T getArgument(final int idx) {
		return getArgument(-1, idx, null);
	}

	/**
	 * Safety return the argument in the position idx.
	 *
	 * If the element class is not of the requested type it returns null and you don't get casting exeption.
	 */
	public <T> T getArgument(final int idx, final Class<? extends T> type) {
		return getArgument(-1, idx, type);
	}

	/**
	 * Safety return the argument in the position idx.
	 *
	 * If the element class is not of the requested type it returns null and you don't get casting exeption.
	 */
	public <T> T getArgument(final int argIdx, final int pos) {
		return getArgument(argIdx, pos, null);
	}

	/**
	 * Utility method for safety getting an object present at a certain position in the list of arguments composed by arrays.
	 *
	 * Useful for Deferred chains where result of each resolved promise is set as an array in the arguments list.
	 *
	 * When the object found in the array doesn't match the type required it returns a null.
	 *
	 * Note: If type is null, we don't check the class of the object found andd you could eventually get a casting exception.
	 *
	 */
	@SuppressWarnings("unchecked")
	public <T> T getArgument(final int argIdx, final int pos, final Class<? extends T> type) {
		final Object[] objs = getArgumentArray(argIdx);
		final Object o = objs.length > pos ? objs[pos] : null;
		if (o != null && (
		// When type is null we don't safety check
		type == null ||
		// The object is an instance of the type requested
				o.getClass() == type ||
				// Overlay types
				type == JavaScriptObject.class && o instanceof JavaScriptObject)) {
			return (T) o;
		}
		return null;
	}

	/**
	 * Utility method for safety getting an array present at a certain position in the list of arguments.
	 *
	 * Useful for Deferred chains where result of each resolved promise is set as an array in the arguments list.
	 *
	 * Always returns an array.
	 */
	public Object[] getArgumentArray(final int idx) {
		final Object o = idx < 0 ? arguments : getArgument(idx);
		if (o != null) {
			return o.getClass().isArray() ? (Object[]) o : new Object[] { o };
		} else if (idx == 0) {
			return arguments;
		}
		return new Object[0];
	}

	/**
	 * Utility method for safety getting a JavaScriptObject present at a certain position in the list of arguments.
	 */
	public <T extends JavaScriptObject> T getArgumentJSO(final int idx) {
		return getArgumentJSO(-1, idx);
	}

	/**
	 * Utility method for safety getting a JavaScriptObject present at a certain position in the list of arguments composed by arrays.
	 *
	 */
	@SuppressWarnings("unchecked")
	public <T extends JavaScriptObject> T getArgumentJSO(final int argIdx, final int pos) {
		return (T) getArgument(argIdx, pos, JavaScriptObject.class);
	}

	public Object[] getArguments() {
		return arguments;
	}

	public <T extends com.google.gwt.dom.client.Element> T getElement() {
		return element.<T> cast();
	}

	public Event getEvent() {
		return event;
	}

	/**
	 * Return the index in a loop execution.
	 *
	 * Used in GQuery.each()
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Set the list of arguments to be passed to the function.
	 */
	public Function setArguments(final Object... arguments) {
		this.arguments = arguments;
		return this;
	}

	public <T extends com.google.gwt.dom.client.Element> Function setElement(final T e) {
		element = e;
		return this;
	}

	public Function setEvent(final Event e) {
		event = e;
		element = e != null ? e.getCurrentEventTarget().<com.google.gwt.dom.client.Element> cast() : null;
		return this;
	}

	public Function setIndex(final int i) {
		index = i;
		return this;
	}

	/**
	 * Utility method to get a string representation with the content of the arguments array. It recursively visits arrays and inspect object to print an appropriate representation of them.
	 *
	 * It is very useful to debug arguments passed in nested promises.
	 *
	 * It is protected so as it can be used in Inner functions.
	 *
	 * Output example:
	 *
	 * <pre>
	 * [0](com.google.gwt.query.client.plugins.QueuePlugin) <div>a1</div><div>a2</div>
	 * [1](com.google.gwt.query.client.plugins.QueuePlugin) <div>a1</div><div>a2</div>
	 * [2](java.lang.String) Foo
	 * [3](JSO) {"bar":"foo"}
	 * </pre>
	 */
	protected String dumpArguments() {
		return dumpArguments(arguments, "\n");
	}

	private String dumpArguments(final Object[] arguments, final String sep) {
		final StringBuilder b = new StringBuilder();
		for (int i = 0, l = arguments.length ; i < l ; i++) {
			b.append("[").append(i).append("]");
			final Object o = arguments[i];
			if (o == null) {
				b.append(" null");
			} else if (o.getClass().isArray()) {
				b.append(dumpArguments((Object[]) o, sep + "   "));
			} else if (o instanceof JavaScriptObject) {
				final JavaScriptObject jso = (JavaScriptObject) o;
				if (JsUtils.isElement(jso)) {
					b.append("(Element) ").append(jso.toString());
				} else {
					b.append("(JSO) ").append(jso.<Properties> cast().toJsonString());
				}
			} else {
				b.append("(").append(o.getClass().getName()).append(") ").append(o);
			}
			if (i < l - 1) {
				b.append(sep);
			}
		}
		return b.toString();
	}
}
