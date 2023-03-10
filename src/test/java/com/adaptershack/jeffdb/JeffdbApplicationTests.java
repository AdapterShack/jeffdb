package com.adaptershack.jeffdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.io.File;
import java.util.List;
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

		try {
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
	
			// run arbitrary queries
			JsonNode results = db.list("foo", (obj) -> obj.get("b").asInt() > obj.get("a").asInt() );
			
			assertEquals(1, results.size());
			
			String id = inserted.get("id").asText();
	
			assertEquals(id, results.get(0).get("id").asText());
	
			results = db.list("foo", (obj) -> obj.get("b").asInt() < obj.get("a").asInt() );
	
			assertEquals(0, results.size());
	
			ObjectNode node2 = objectMapper.createObjectNode();
			node2.put("name", "Jeff");
			db.insert("bar", node2);
			
			assertEquals(1, db.listAll("bar").size());
			assertEquals("Jeff",  db.listAll("bar").get(0).get("name").asText());
			
			db.deleteMatching("bar", obj -> obj.get("name").asText().equalsIgnoreCase("jeff") );
			
			assertEquals(0, db.listAll("bar").size());
			
			assertTrue( new File( root ).exists());
			
			assertTrue( new File( root, "foo").exists());
			
			assertTrue( new File( new File( root, "foo"), id+".json").exists());
			
			// serialize any object that has getters/setters for a String "id"
			IdHavingObject obj = new IdHavingObject();
			obj.setName("jeff");
			
			IdHavingObject insertedObj = db.insert("baz", obj);
			
			assertNotNull( insertedObj.getId() );
			
			List<IdHavingObject> list = db.listAll("baz", IdHavingObject.class);
			
			assertEquals(insertedObj.getId(), list.get(0).getId());
			assertEquals("jeff", list.get(0).getName());

			obj.setName("Jeff");

			db.update("baz",insertedObj.getId(), obj);
			
			db.insert("baz", new IdHavingObject("ffej") );

			assertEquals(2, db.listAll("baz", IdHavingObject.class).size());
			assertEquals(2, db.listAll("baz").size());
			
			List<IdHavingObject> list2 =
				db.list("baz", IdHavingObject.class, o -> o.getName().equalsIgnoreCase("jeff"));
			
			assertEquals(1, list2.size());
			assertEquals("Jeff", list2.get(0).getName());
			
			db.archive("baz");
			
			assertEquals(0, db.listAll("baz").size() );

			assertNotEquals(0, new File( new File(db.getRootDirectory(), "baz"), db.getArchiveName() ).listFiles().length );
			
		} finally {
		
			FileSystemUtils.deleteRecursively(new File(root));		

		}
		
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
	
	public static class IdHavingObject {
		
		String id;
		
		String name;
		
		
		public IdHavingObject() {
		}

		public IdHavingObject(String id, String name) {
			super();
			this.id = id;
			this.name = name;
		}

		public IdHavingObject(String name) {
			this.name = name;
		}
		
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		
		
		
	}
	
	
}
