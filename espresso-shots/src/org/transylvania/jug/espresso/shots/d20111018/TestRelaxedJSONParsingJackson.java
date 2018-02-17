package org.transylvania.jug.espresso.shots.d20111018;

import static org.junit.Assert.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.*;

public class TestRelaxedJSONParsingJackson {
	@Test
	public void testLeniendParsing() throws Exception {
		JsonParser parser = new JsonFactory()
			.createJsonParser("[{ foo: 'bar' }]")
				.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
				.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
		JsonNode root = new ObjectMapper().readTree(parser);
		
		assertEquals("bar", root.get(0).get("foo").asText());
	}
}
