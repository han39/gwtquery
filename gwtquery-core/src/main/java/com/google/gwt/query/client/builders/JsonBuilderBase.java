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
package com.google.gwt.query.client.builders;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.query.client.GQ;
import com.google.gwt.query.client.IsProperties;
import com.google.gwt.query.client.Properties;
import com.google.gwt.query.client.js.JsCache;
import com.google.gwt.query.client.js.JsObjectArray;
import com.google.gwt.query.client.js.JsUtils;

/**
 * Common class for all JsonBuilder implementations.
 *
 * @param <J>
 */
public abstract class JsonBuilderBase<J extends JsonBuilderBase<?>> implements JsonBuilder {

	protected Properties p = Properties.create();
	protected String[] fieldNames = new String[] {};

	@Override
	public <T extends JsonBuilder> T as(final Class<T> clz) {
		return p.as(clz);
	}

	@Override
	public <T> T get(final Object key) {
		return p.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Properties getDataImpl() {
		return p;
	}

	@Override
	public final String[] getFieldNames() {
		return fieldNames;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Properties getProperties() {
		return p;
	}

	@SuppressWarnings("unchecked")
	@Override
	public J load(final Object prp) {
		assert prp == null || prp instanceof JavaScriptObject || prp instanceof String;
		if (prp != null && prp instanceof String) {
			return parse((String) prp);
		}
		if (prp != null) {
			p = (Properties) prp;
		}
		return (J) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public J parse(final String json) {
		return load(JsUtils.parseJSON(json));
	}

	@SuppressWarnings("unchecked")
	@Override
	public J parse(final String json, final boolean fix) {
		return fix ? parse(Properties.wrapPropertiesString(json)) : parse(json);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IsProperties> T set(final Object key, final Object val) {
		if (val instanceof IsProperties) {
			p.set(key, ((IsProperties) val).getDataImpl());
		} else if (val instanceof Object[]) {
			setArrayBase(String.valueOf(key), (Object[]) val);
		} else if (val instanceof Collection) {
			final Collection<?> collection = (Collection<?>) val;
			setArrayBase(String.valueOf(key), collection.toArray(new Object[collection.size()]));
		} else {
			p.set(key, val);
		}
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public J strip() {
		final List<String> names = Arrays.asList(getFieldNames());
		for (final String jsonName : p.getFieldNames()) {
			// TODO: figure out a way so as we can generate some marks in generated class in
			// order to call getters to return JsonBuilder object given an an attribute name
			if (!names.contains(jsonName)) {
				p.<JsCache> cast().delete(jsonName);
			}
		}
		return (J) this;
	}

	@Override
	public String toJson() {
		return p.tostring();
	}

	@Override
	public String toJsonWithName() {
		return "{\"" + getJsonName() + "\":" + p.tostring() + "}";
	}

	@Override
	public String toQueryString() {
		return p.toQueryString();
	}

	@Override
	public String toString() {
		return p.tostring();
	}

	@SuppressWarnings("unchecked")
	protected <T> T[] getArrayBase(final String n, final T[] r, final Class<T> clazz) {
		final JsObjectArray<?> a = p.getArray(n).cast();
		final int l = r.length;
		for (int i = 0 ; i < l ; i++) {
			final Object w = a.get(i);
			Class<?> c = w.getClass();
			do {
				if (c.equals(clazz)) {
					r[i] = (T) w;
					break;
				}
				c = c.getSuperclass();
			} while (c != null);
		}
		return r;
	}

	protected final <T extends JsonBuilder> T[] getIsPropertiesArrayBase(final JsArrayMixed js, final T[] r, final Class<T> clazz) {
		final JsObjectArray<?> a1 = js.cast();
		for (int i = 0 ; i < r.length ; i++) {
			r[i] = getIsPropertiesBase(a1.get(i), clazz);
		}
		return r;
	}

	protected final <T extends JsonBuilder> T getIsPropertiesBase(final Object o, final Class<T> clazz) {
		return GQ.create(clazz).load(o);
	}

	protected Properties getPropertiesBase(final String n) {
		if (p.getJavaScriptObject(n) == null) {
			p.set(n, Properties.create());
		}
		return p.getJavaScriptObject(n);
	}

	protected <T> void setArrayBase(final String n, final T[] r) {
		if (r.length > 0 && r[0] instanceof JsonBuilder) {
			final JsArray<JavaScriptObject> a = JavaScriptObject.createArray().cast();
			for (final T o : r) {
				a.push(((JsonBuilder) o).<Properties> getDataImpl());
			}
			p.set(n, a);
		} else {
			final JsObjectArray<Object> a = JsObjectArray.create();
			a.add(r);
			p.set(n, a);
		}
	}
}
