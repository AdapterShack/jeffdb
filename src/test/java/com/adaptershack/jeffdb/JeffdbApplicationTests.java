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
import org.springframework.util.FileSystemUtils;

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
		
		// create a new name every time
		String collection = randomName();

		// it should not already exist
		assertFalse( new File( controller.db.getRootDirectory(), collection).exists());
		
		// it should be empty
		assertEquals(0, controller.listAll(collection).size());

		// it should be empty
		assertTrue( new File( controller.db.getRootDirectory(), collection).exists());
		
		// make some objects that we plan to insert
		ObjectNode row = row(1,"two");
		ObjectNode row2 = row(3,"four");
		ObjectNode row3 = row(5,"six");
		
		// insert the first one
		JsonNode inserted = controller.insert(collection, row);

		// collection size should now be 1
		assertEquals(1, controller.listAll(collection).size());		

		// the data inserted should match the data we passed in
		assertTrue( inserted.get("id").isTextual() );
		assertEquals( 1, inserted.get("a").asInt());
		assertEquals( "two", inserted.get("b").asText());

		String id = inserted.get("id").asText();
		
		// get by id

		JsonNode retrieved = controller.get(collection, id).getBody();
		
		assertEquals( 1, retrieved.get("a").asInt());
		assertEquals( "two", retrieved.get("b").asText());

		// insert another object, collection size should now be two
		
		controller.insert(collection, row2);
		
		assertEquals(2, controller.listAll(collection).size());
		
		// try changing one of them and update it in dbase
		row.put("a",1);
		row.put("b", "bleh");
		
		controller.put(collection, id, row);
		assertEquals("bleh", controller.get(collection, id).getBody().get("b").asText());

		// finding an object by one of its other fields besides ID
		ObjectNode finder = objectMapper.createObjectNode();
		finder.put("a", 1);
		
		JsonNode found = controller.postSearch(collection, finder);
		
		assertEquals(1, found.size());
		assertEquals(id, found.get(0).get("id").asText());
		assertEquals(1, found.get(0).get("a").asInt());
		assertEquals("bleh", found.get(0).get("b").asText());

		// delete by ID and ensure we deleted the right one
		controller.delete(collection, id);
		
		assertEquals(1, controller.listAll(collection).size());		

		assertEquals(3, controller.listAll(collection).get(0).get("a").asInt());
		assertEquals("four", controller.listAll(collection).get(0).get("b").asText());

		// insert another
		controller.insert(collection, row3);
		
		assertEquals(2, controller.listAll(collection).size());		

		finder = objectMapper.createObjectNode();
		finder.put("b", "four");		
		
		// delete by field
		controller.deleteSearch(collection,finder);
		
		assertEquals(1, controller.listAll(collection).size());		

		assertEquals(5, controller.listAll(collection).get(0).get("a").asInt());
		assertEquals("six", controller.listAll(collection).get(0).get("b").asText());
		
		
		// delete the entire collection
		controller.deleteCollection(collection);

		assertFalse( new File( controller.db.getRootDirectory(), collection).exists());
		
	}
	
	@Test
	void testShenanigans() {
		
		String badName = "!@#$%^&*(";
		
		try {
			controller.listAll(badName);
		} catch (IllegalArgumentException e) {
			// good!
		}

		assertFalse( new File( controller.db.getRootDirectory(), badName).exists());
		
	}

	@Test
	void testEmbeddedUse() {
		
		String root = randomName();
		ObjectMapper objectMapper = controller.db.objectMapper;

		assertFalse( new File( root ).exists());
		
		DatabaseService db = new DatabaseService();
		
		// set whatever directory you want the files to go in
		db.setRootDirectory(root);
		
		// whatever instance of Jackson's ObjectMapper your project already uses
		db.setObjectMapper(objectMapper);

		// junk up some data
		ObjectNode node = objectMapper.createObjectNode();
		node.put("a", 1);
		node.put("b", 2);

		// insert it in there
		JsonNode inserted = db.insert("foo", node);
		
		String id = inserted.get("id").asText();
		
		assertTrue( new File( root ).exists());
		
		assertTrue( new File( root, "foo").exists());
		
		assertTrue( new File( new File( root, "foo"), id+".json").exists());
		
		FileSystemUtils.deleteRecursively(new File(root));		

		assertFalse( new File( root ).exists());
		
		
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
