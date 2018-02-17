package org.transylvania.jug.espresso.shots.d20111018;

import java.io.StringReader;
import java.lang.reflect.Type;

import static org.junit.Assert.*;
import java.util.*;

import org.junit.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

public class TestRelaxedJSONParsing {
	@Test
	public void testLeniendParsing() throws Exception {
		Object parsed = parseJSON("[{ foo: 'bar' }]");
		List<Map<String, String>> expected 
			= Collections.singletonList(Collections.singletonMap("foo", "bar"));
		assertEquals(expected, parsed);
	}
	
	private Object parseJSON(String json) throws JsonIOException {
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Holder.class, new Deserializer())
			.create();
		JsonReader reader = new JsonReader(new StringReader(json));		
		reader.setLenient(true);
		Holder holder = gson.fromJson(reader, Holder.class);
		if (null == holder) { return null; }
		return holder.object;
	}
	
	private static class Holder {
		private final Object object;
		
		private Holder(Object object) {
			this.object = object;
		}
	}
	
	private class Deserializer implements JsonDeserializer<Holder> {
		private Object toObject(JsonElement jsonElement) {
			if (jsonElement.isJsonObject()) {
				Map<String, Object> result = new HashMap<String, Object>();
				for (Object entry : ((JsonObject)jsonElement).entrySet()) {
					@SuppressWarnings("unchecked")
					Map.Entry<String, JsonElement> mapEntry = (Map.Entry<String, JsonElement>)entry;
					result.put(mapEntry.getKey(), toObject(mapEntry.getValue()));
				}
				return Collections.unmodifiableMap(result);
			}
			else if (jsonElement.isJsonArray()) {
				JsonArray jsonArray = (JsonArray)jsonElement;
				List<Object> result = new ArrayList<Object>(jsonArray.size());
				for (JsonElement e : jsonArray) {
					result.add(toObject(e));
				}
				return Collections.unmodifiableList(result);
			}
			else if (jsonElement.isJsonPrimitive()) {
				JsonPrimitive jsonPrimitive = (JsonPrimitive)jsonElement;
				if (jsonPrimitive.isBoolean()) {
					return jsonPrimitive.getAsBoolean();
				} 
				else if (jsonPrimitive.isNumber()) {
					return jsonPrimitive.getAsNumber();
				}
				else if (jsonPrimitive.isString()) {
					return jsonPrimitive.getAsString();
				}
				else {
					throw new IllegalArgumentException("Received class for which no handling code exists: " + jsonElement);
				}
			}
			else if (jsonElement.isJsonNull()) {
				return null;
			}
			else {
				throw new IllegalArgumentException("Received class for which no handling code exists: " + jsonElement);
			}
		}

		@Override
		public Holder deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			return new Holder(toObject(json));
		}
		
	}
}
