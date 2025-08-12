/*
 * Copyright 2014, The gwtquery team.
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
package com.google.gwt.query.vm;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import com.google.gson.JsonNull;
import com.google.gwt.dev.json.JsonBoolean;
import com.google.gwt.dev.json.JsonNumber;
import com.google.gwt.dev.json.JsonString;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.IsProperties;
import com.google.gwt.query.client.Properties;
import com.google.gwt.query.client.builders.JsonBuilder;
import com.google.gwt.query.client.builders.Name;
import com.google.gwt.query.rebind.JsonBuilderGenerator;
import com.google.gwt.query.vm.JsonFactoryJre.JreJsonFunction;

/**
 * Reflection handler for JsonBuilder implementation in JVM.
 */
public class JsonBuilderHandler implements InvocationHandler {

	static JsonFactoryJre jsonFactory = new JsonFactoryJre();

	private JSONObject jsonObject;

	public JsonBuilderHandler() {
		jsonObject = new JSONObject();
	}

	public JsonBuilderHandler(final JSONObject j) {
		jsonObject = j;
	}

	public JsonBuilderHandler(final String payload) throws Throwable {
		jsonObject = JSONParser.parseStrict(payload).isObject();
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		final String mname = method.getName();
		final Class<?>[] classes = method.getParameterTypes();
		final int largs = classes.length;

		final Name name = method.getAnnotation(Name.class);
		String attr = name != null ? name.value() : methodName2AttrName(mname);

		if ("getFieldNames".equals(mname)) {
			// Je ne vois pas le truc !
			return "";
		} else if ("as".equals(mname)) {
			@SuppressWarnings("unchecked")
			final Class<? extends JsonBuilder> clz = (Class<? extends JsonBuilder>) args[0];
			return jsonFactory.create(clz, jsonObject);
		} else if ("getJsonName".equals(mname)) {
			return JsonBuilderGenerator.classNameToJsonName(getDataBindingClassName(proxy.getClass()));
		} else if (mname.matches("getProperties|getDataImpl")) {
			return jsonObject;
		} else if (largs > 0 && ("parse".equals(mname) || "load".equals(mname))) {
			String json = String.valueOf(args[0]);
			if (largs > 1 && Boolean.TRUE.equals(args[1])) {
				json = Properties.wrapPropertiesString(json);
			}
			jsonObject = JSONParser.parseStrict(json).isObject();
		} else if ("strip".equals(mname)) {
			stripProxy((JsonBuilder) proxy);
		} else if (mname.matches("toString")) {
			return jsonObject.toString();
		} else if (mname.matches("toJsonWithName")) {
			final String jsonName = JsonBuilderGenerator.classNameToJsonName(getDataBindingClassName(proxy.getClass()));
			return "{\"" + jsonName + "\":" + jsonObject.toString() + "}";
		} else if (mname.matches("toJson")) {
			return jsonObject.toString();
		} else if ("toQueryString".equals(mname)) {
			return param(jsonObject);
		} else if (largs == 1 && mname.equals("get")) {
			final Class<?> ret = method.getReturnType();
			attr = String.valueOf(args[0]);
			return getValue(null, 0, jsonObject, attr, ret, method);
		} else if (largs == 0 || mname.startsWith("get")) {
			final Class<?> ret = method.getReturnType();
			return getValue(null, 0, jsonObject, attr, ret, method);
		} else if (largs == 2 && mname.equals("set")) {
			setValue(null, jsonObject, String.valueOf(args[0]), args[1]);
			return proxy;
		} else if (largs == 1 || mname.startsWith("set")) {
			setValue(null, jsonObject, attr, args[0]);
			return proxy;
		}
		return null;
	}

	public String methodName2AttrName(final String s) {
		return deCapitalize(s.replaceFirst("^[gs]et", ""));
	}

	private String deCapitalize(final String s) {
		return s != null && s.length() > 0 ? s.substring(0, 1).toLowerCase() + s.substring(1) : s;
	}

	private HashSet<String> getAttributeNames(final Method[] methods) {
		final HashSet<String> valid = new HashSet<>();

		if (methods == null || methods.length == 0) {
			return valid;
		}

		for (final Method m : methods) {
			String attr = methodName2AttrName(m.getName());
			final Name annotation = m.getAnnotation(Name.class);

			if (annotation != null) {
				attr = annotation.value();
			}
			valid.add(attr);
		}
		return valid;
	}

	private String getDataBindingClassName(final Class<?> type) {
		for (final Class<?> c : type.getInterfaces()) {
			if (c.equals(JsonBuilder.class)) {
				return type.getName();
			} else {
				return getDataBindingClassName(c);
			}
		}
		return null;
	}

	private Hashtable<String, Method> getJsonBuilders(final Method[] methods) {
		final Hashtable<String, Method> ispropertyGetters = new Hashtable<>();

		if (methods == null || methods.length == 0) {
			return ispropertyGetters;
		}

		for (final Method m : methods) {
			final Class<?>[] classes = m.getParameterTypes();
			final boolean isJsonBuilder = classes.length == 0 && IsProperties.class.isAssignableFrom(m.getReturnType());
			if (isJsonBuilder) {
				final String attr = methodName2AttrName(m.getName());
				ispropertyGetters.put(attr, m);
			}
		}

		return ispropertyGetters;
	}

	private Object getValue(final JSONArray arr, final int idx, final JSONObject obj, final String attr, final Class<?> clz,
			final Method method) {
		if (clz.equals(Boolean.class) || clz == Boolean.TYPE) {
			try {
				return obj != null ? ((JSONBoolean) obj.get(attr)).booleanValue() : ((JSONBoolean) arr.get(idx)).booleanValue();
			} catch (final Exception e) {
				return Boolean.FALSE;
			}
		} else if (clz.equals(Date.class)) {
			return new Date((long) (obj != null ? ((JSONNumber) obj.get(attr)).doubleValue() : ((JSONNumber) arr.get(idx)).doubleValue()));
		} else if (clz.equals(Byte.class) || clz == Byte.TYPE) {
			return toDouble(attr, arr, idx, obj).byteValue();
		} else if (clz.equals(Short.class) || clz == Short.TYPE) {
			return toDouble(attr, arr, idx, obj).shortValue();
		} else if (clz.equals(Integer.class) || clz == Integer.TYPE) {
			return toDouble(attr, arr, idx, obj).intValue();
		} else if (clz.equals(Double.class) || clz == Double.TYPE) {
			return toDouble(attr, arr, idx, obj);
		} else if (clz.equals(Float.class) || clz == Float.TYPE) {
			return toDouble(attr, arr, idx, obj).floatValue();
		} else if (clz.equals(Long.class) || clz == Long.TYPE) {
			return toDouble(attr, arr, idx, obj).longValue();
		}

		final Object ret = obj != null ? obj.get(attr) : arr.get(idx);
		if (ret instanceof JreJsonFunction || clz.equals(Function.class)) {
			return ret != null && ret instanceof JreJsonFunction ? ((JreJsonFunction) ret).getFunction()
					: null;
		} else if (ret instanceof JsonNull) {
			return null;
		} else if (ret instanceof JsonString) {
			return ((JsonString) ret).asString();
		} else if (ret instanceof JsonBoolean) {
			return ((JsonBoolean) ret).asBoolean();
		} else if (ret instanceof JsonNumber) {
			return toDouble(attr, arr, idx, obj);
		} else if (ret instanceof JSONArray || clz.isArray() || clz.equals(List.class)) {
			Class<?> ctype = Object.class;
			if (clz.isArray()) {
				ctype = clz.getComponentType();
			} else {
				final Type returnType = method.getGenericReturnType();
				if (returnType instanceof ParameterizedType) {
					ctype = (Class<?>) ((ParameterizedType) returnType).getActualTypeArguments()[0];
				}
			}
			return jsonArrayToList(obj.get(attr).isArray(), ctype, clz.isArray());
		} else if (ret instanceof JSONObject) {
			if (clz == Object.class) {
				return jsonFactory.createBinder((JSONObject) ret);
			} else if (IsProperties.class.isAssignableFrom(clz) && !clz.isAssignableFrom(ret.getClass())) {
				return jsonFactory.create(clz, (JSONObject) ret);
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private <T> Object jsonArrayToList(final JSONArray j, final Class<T> ctype, final boolean isArray) {
		if (j == null) {
			return null;
		}

		final List<T> l = new ArrayList<>();
		for (int i = 0 ; j != null && i < j.size() ; i++) {
			l.add((T) getValue(j, i, null, null, ctype, null));
		}

		return l.isEmpty() ? Collections.emptyList() : isArray ? l.toArray((T[]) Array.newInstance(ctype, l.size())) : l;
	}

	private <T> JSONArray listToJsonArray(final Object... l) throws Throwable {
		final JSONArray ret = new JSONArray();
		for (final Object o : l) {
			setValue(ret, null, null, o);
		}
		return ret;
	}

	private String param(final JSONObject o) {
		String ret = "";
		for (final String k : o.keySet()) {
			ret += ret.isEmpty() ? "" : "&";
			final JSONValue v = o.get(k);
			if (v instanceof JSONArray) {
				for (int i = 0, l = ((JSONArray) v).size() ; i < l ; i++) {
					ret += i > 0 ? "&" : "";
					final JSONValue e = ((JSONArray) v).get(i);
					ret += k + "[]=" + e.toString();
				}
			} else {
				if (v != null && !(v instanceof JSONNull)) {
					ret += k + "=" + v.toString();
				}
			}
		}
		return ret;
	}

	private Object setValue(final JSONArray jsArr, final JSONObject jsObj, final String attr, Object val) {
		if (val == null) {
			return JSONNull.getInstance();
		}

		try {
			Class<?> valClaz = JSONValue.class;
			if (val instanceof Number) {
				val = ((Number) val).doubleValue();
				valClaz = Double.TYPE;
			} else if (val instanceof Boolean) {
				valClaz = Boolean.TYPE;
			} else if (val instanceof Date) {
				val = ((Date) val).getTime();
				valClaz = Double.TYPE;
			} else if (val instanceof String) {
				valClaz = String.class;
			} else if (val instanceof IsProperties) {
				val = ((IsProperties) val).getDataImpl();
			} else if (val.getClass().isArray() || val instanceof List) {
				val = listToJsonArray(val.getClass().isArray() ? (Object[]) val : ((List<?>) val).toArray());
			} else if (val instanceof Function) {
				val = new JreJsonFunction((Function) val);
			}

			if (jsObj != null) {
				final Method mth = jsObj.getClass().getMethod("put", String.class, valClaz);
				mth.invoke(jsObj, new Object[] { attr, val });
				return jsObj;
			} else {
				final Method mth = jsArr.getClass().getMethod("set", Integer.TYPE, valClaz);
				mth.invoke(jsArr, new Object[] { Integer.valueOf(jsArr.size()), val });
				return jsArr;
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * This method removes all the json which is not mapped into a method inside the JsonBuilder Object. Also if the proxy contains another JsonBuilder in their methods the method strip() is called.
	 */
	private void stripProxy(final JsonBuilder proxy) throws Throwable {
		final Class<?> type = proxy.getClass().getInterfaces()[0];

		final HashSet<String> validAttrs = getAttributeNames(type.getMethods());
		final Hashtable<String, Method> ispropertyGetters = getJsonBuilders(type.getMethods());

		for (final String key : jsonObject.keySet()) {
			final String name = methodName2AttrName(key);
			if (!validAttrs.contains(name)) {
				jsonObject.put(key, JSONNull.getInstance());
				continue;
			}
			final Method ispropertyGetter = ispropertyGetters.get(name);
			if (ispropertyGetter != null) {
				((IsProperties) invoke(proxy, ispropertyGetter, new Object[] {})).strip();
			}
		}
	}

	private Double toDouble(final String attr, final JSONArray arr, final int idx, final JSONObject obj) {
		try {
			return obj != null ? ((JSONNumber) obj.get(attr)).doubleValue() : ((JSONNumber) arr.get(idx)).doubleValue();
		} catch (final Exception e) {
			return 0d;
		}
	}
}
