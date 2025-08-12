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
package com.google.gwt.query.client.js;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Lightweight JSO based array class that can store objects.
 *
 * @param <T>
 */
public final class JsObjectArray<T> extends JavaScriptObject {

	public static <T> JsObjectArray<T> create() {
		return JavaScriptObject.createArray().cast();
	}

	protected JsObjectArray() {}

	public JsObjectArray<T> add(final int i, final T val) {
		c().put(i, val);
		return this;
	}

	@SuppressWarnings("unchecked")
	public JsObjectArray<T> add(final T... vals) {
		for (final T t : vals) {
			if (t instanceof Number) {
				c().putNumber(length(), ((Number) t).doubleValue());
			} else if (t instanceof Boolean) {
				c().putBoolean(length(), (Boolean) t);
			} else {
				c().put(length(), t);
			}
		}
		return this;
	}

	public void concat(final JsObjectArray<T> ary) {
		c().concat(ary);
	}

	public boolean contains(final Object o) {
		return c().contains(o);
	}

	public Object[] elements() {
		return c().elements();
	}

	@SuppressWarnings("unchecked")
	public T get(final int index) {
		return (T) c().get(index);
	}

	public int length() {
		return c().length();
	}

	public void pushAll(final JavaScriptObject prevElem) {
		c().pushAll(prevElem);
	}

	public void remove(final Object... objects) {
		for (final Object o : objects) {
			c().remove(o);
		}
	}

	public void set(final int i, final T val) {
		c().put(i, val);
	}

	private JsCache c() {
		return cast();
	}
}
