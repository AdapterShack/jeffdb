package com.adaptershack.jeffdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest
class JeffdbApplicationTests {

	@Autowired
	Controller controller;
	
	@Autowired
	ObjectMapper objectMapper;
	
	@Test
	void contextLoads() {
	}
	
	@Test
	void testCrud() {
		
		String collection = randomName();

		assertFalse( new File( controller.db.getRootDirectory(), collection).exists());
		
		assertEquals(0, controller.listAll(collection).size());

		assertTrue( new File( controller.db.getRootDirectory(), collection).exists());
		
		
		ObjectNode row = row(1,"two");
		ObjectNode row2 = row(3,"four");
		ObjectNode row3 = row(5,"six");
		
		JsonNode inserted = controller.insert(collection, row);

		assertEquals(1, controller.listAll(collection).size());		
		
		assertTrue( inserted.get("id").isTextual() );

		assertEquals( 1, inserted.get("a").asInt());
		assertEquals( "two", inserted.get("b").asText());

		String id = inserted.get("id").asText();
		
		JsonNode retrieved = controller.get(collection, id).getBody();
		
		assertEquals( 1, retrieved.get("a").asInt());
		assertEquals( "two", retrieved.get("b").asText());
		
		controller.insert(collection, row2);
		
		assertEquals(2, controller.listAll(collection).size());
		
		row.put("a",1);
		row.put("b", "bleh");
		
		controller.put(collection, id, row);
		assertEquals("bleh", controller.get(collection, id).getBody().get("b").asText());
		
		ObjectNode finder = objectMapper.createObjectNode();
		finder.put("a", 1);
		
		JsonNode found = controller.postSearch(collection, finder);
		
		assertEquals(1, found.size());
		assertEquals(id, found.get(0).get("id").asText());
		assertEquals(1, found.get(0).get("a").asInt());
		assertEquals("bleh", found.get(0).get("b").asText());
		
		controller.delete(collection, id);
		
		assertEquals(1, controller.listAll(collection).size());		

		assertEquals(3, controller.listAll(collection).get(0).get("a").asInt());
		assertEquals("four", controller.listAll(collection).get(0).get("b").asText());

		controller.insert(collection, row3);
		
		assertEquals(2, controller.listAll(collection).size());		

		finder = objectMapper.createObjectNode();
		finder.put("b", "four");		
		
		controller.deleteSearch(collection,finder);
		
		assertEquals(1, controller.listAll(collection).size());		

		assertEquals(5, controller.listAll(collection).get(0).get("a").asInt());
		assertEquals("six", controller.listAll(collection).get(0).get("b").asText());
		
		controller.deleteCollection(collection);

		assertFalse( new File( controller.db.getRootDirectory(), collection).exists());
		
	}

	private String randomName() {
		String collection = UUID.randomUUID().toString().replaceAll("-","");
		return collection;
	}

	private ObjectNode row(int a, String b) {
		ObjectNode row = objectMapper.createObjectNode();
		row.put("a",a);
		row.put("b",b);
		
		return row;
	}
	
}
