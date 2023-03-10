package com.adaptershack.jeffdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Main implementation of all database CRUD functions.
 * 
 * @author Jeff
 *
 */
@Service
public class DatabaseService {
	
	/* The root directory where all files live.
	 * When running in Spring Boot this is injected from the properties file.
	 */
	@Value("${com.adaptershack.jeffdb.root:.jeffdb.data}")
	String rootDirectory;
	
	/*
	 * This inject's Spring's own single instance of ObjectMapper
	 */
	@Autowired
	ObjectMapper objectMapper;

	public String getArchiveName() {
		return archiveName;
	}

	public void setArchiveName(String archiveName) {
		this.archiveName = archiveName;
	}

	@Value("${com.adaptershack.jeffdb.root:archive}")
	private String archiveName = "archive";
	
	/*
	 * Getters and setters, for manual (non-injected) usage.
	 */
	
	public String getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	
	

	/**
	 * Adds a new object to the specified collections.
	 * If the object already contains a key called "id",
	 * then the value of this field will be used to store
	 * and retrieve the object. If no such field exists,
	 * one will be randomly generated.
	 * 
	 * @param collection
	 * @param row
	 * @return
	 */
	public ObjectNode insert(String collection, ObjectNode row) {
		
		if(!row.has(ID)) {
			row.put(ID, generateId());
		}
		
		File collectionDir = directoryExists(collection);
		
		String id = row.get(ID).asText();
		
		checkRegex(id);
		
		File rowFile = new File( collectionDir, id + DOT_JSON );
		
		try {
			objectMapper.writeValue(rowFile, row);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return row;
		
	}

	
	/**
	 * Adds a Java object to the collection by first serializing it
	 * as Json.
	 * 
	 * The object does not need to extend any particular
	 * superclass or implement any interface, but it must have a public
	 * getter/setter for a String id. 
	 * 
	 * @param <T>
	 * @param collection
	 * @param row
	 * @return
	 */
	public <T> T insert(String collection, T row) {
		
		File collectionDir = directoryExists(collection);
		
		String id = null;
		
		try {
			id = (String) PropertyUtils.getProperty(row, ID);
			
			if(id == null) {
				id = generateId();
				PropertyUtils.setProperty(row,ID, id);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		
		checkRegex(id);
		
		File rowFile = new File( collectionDir, id + DOT_JSON );
		
		try {
			objectMapper.writeValue(rowFile, row);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return row;
		
	}
	
	

	/**
	 * Stores the object under the specified ID.
	 * 
	 * @param collection
	 * @param id
	 * @param row
	 * @return
	 */
	public ObjectNode update(String collection, String id, ObjectNode row) {

		row.put(ID, id);
		
		return insert(collection,row);
		
	}

	/**
	 * Stores any object under the specified ID by serializing it as Json.
	 * 
	 * The object does not need to extend any particular
	 * superclass or implement any interface, but it must have a public
	 * getter/setter for a String id. 
	 * 
	 * @param <T>
	 * 
	 * @param collection
	 * @param id
	 * @param row
	 * @return
	 */
	public <T> T update(String collection, String id, T row) {

		try {
			PropertyUtils.setProperty(row,ID, id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return insert(collection,row);
		
	}
	
	
	
	/**
	 * Gets the object specified by the id, from the specified collection.
	 * 
	 * @param collection
	 * @param id
	 * @return
	 */
	public ObjectNode get(String collection, String id) {

		checkRegex(id);

		File collectionDir = directoryExists(collection);
		
		File rowFile = new File( collectionDir, id + DOT_JSON );
		
		if(!rowFile.exists()) {
			return null;
		}
		
		try {
			return (ObjectNode) objectMapper.readTree(rowFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	/**
	 * Gets the object specified by the id, from the specified collection.
	 * @param <T>
	 * 
	 * @param collection
	 * @param id
	 * @return
	 */
	public <T> T get(String collection, String id, Class<T> clazz) {

		checkRegex(id);

		File collectionDir = directoryExists(collection);
		
		File rowFile = new File( collectionDir, id + DOT_JSON );
		
		if(!rowFile.exists()) {
			return null;
		}
		
		try {
			return objectMapper.readValue(rowFile,clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	
	
	/**
	 * Deletes the object specified by the ID.
	 * 
	 * @param collection
	 * @param id
	 * @return
	 */
	public boolean delete(String collection, String id) {
		
		checkRegex(id);

		File collectionDir = directoryExists(collection);

		File rowFile = new File( collectionDir, id + DOT_JSON );
		
		if(!rowFile.exists()) {
			return false;
		} else {
			return rowFile.delete();
		}
		
	}
	
	
	/**
	 * Simply gets all objects from the specified collection.
	 * 
	 * @param collection
	 * @return
	 */
	public ArrayNode listAll(String collection) {
		return list(collection,null);
	}

	

	
	/**
	 * Deserializes all the Json data in the collection to a list of objects of the specified
	 * class.
	 * 
	 * @param <T>
	 * @param collection
	 * @param clazz
	 * @param predicate
	 * @return
	 */
	public <T> List<T> listAll(String collection, Class<T> clazz ) {
		return list(collection,clazz,null);
	}
	
	/**
	 * Deserializes all the Json data in the collection to a list of objects of the specified
	 * class, subject to the predicate returning true.
	 * 
	 * Example:
	 * 
	 * list("foo", MyClass.class, o -> o.getName().equalsIgnoreCase("jeff") );
	 * 
	 * @param <T>
	 * @param collection
	 * @param clazz
	 * @param predicate
	 * @return
	 */
	public <T> List<T> list(String collection, Class<T> clazz, Predicate<T> predicate ) {

		List<T> list = new ArrayList<T>();
		list(collection,list,clazz,predicate);
		return list;
	}
	
	
	/**
	 * Deserializes all the Json data in the collection to a list of objects of the specified
	 * class, tests each using the specified predicate, and adds the objects to the specified
	 * container if true.
	 * 
	 * Example:
	 * 
	 * list("foo", myList, MyClass.class, o -> o.getName().equalsIgnoreCase("jeff") );
	 * 
	 * @param <T>
	 * @param collection
	 * @param clazz
	 * @param predicate
	 * @return
	 */
	public <T> void list(String collection, Collection<T> list, Class<T> clazz, Predicate<T> predicate ) {

		readAll(collection, clazz, obj ->
		{
			if(predicate == null || predicate.test(obj)) {
				list.add(obj);
			}
			
		});
		
	}

	
	
	/**
	 * Finds all objects in the collection where the predicate returns true.
	 * 
	 * Example:
	 * 
     *   list("bar", obj -> obj.get("name").asText().equalsIgnoreCase("jeff") );
	 * 
	 * 
	 * @param collection
	 * @param predicate
	 * @return
	 */
	public ArrayNode list(String collection, Predicate<JsonNode> predicate ) {

		ArrayNode list = objectMapper.createArrayNode();
		
		readAll(collection, row -> {
			if(predicate == null || predicate.test(row)) {
				list.add(row);
			}
		});
		
		return list;
	}


	/**
	 * Finds and DELETES all objects in the collection where the predicate returns true.
	 * 
	 * Example:
	 * 
     *   deleteMatching("bar", obj -> obj.get("name").asText().equalsIgnoreCase("jeff") );
	 * 
	 * @param collection
	 * @param predicate
	 * @return
	 */
	public boolean deleteMatching(String collection, Predicate<JsonNode> predicate ) {

		JsonNode removed = list(collection, (row) ->
			{ 
				if(predicate.test(row)) {
					delete(collection,row.get(ID).asText());
					return true;
				} else {
					return false;
				}
			});
		
		return ! removed.isEmpty();
	}
	
	/**
	 * A convenience method for finding objects where the same keys,
	 * having the same values, exist as in the provided object.
	 * 
	 * The query object need not have as many fields as the objects
	 * in the collection. The most common use case is probably for it
	 * to have only one field.
	 * 
	 * @param collection
	 * @param params
	 * @return
	 */
	public ArrayNode listMatching(String collection, JsonNode params) {
		return list(collection, new MatchingP(params));
	}
	
	/**
	 * A convenience method for DELETING objects where the same keys,
	 * having the same values, exist as in the provided object.
	 * 
	 * The query object need not have as many fields as the objects
	 * in the collection. The most common use case is probably for it
	 * to have only one field.
	 * 
	 * @param collection
	 * @param params
	 * @return
	 */
	public boolean deleteMatching(String collection, JsonNode params) {
		return deleteMatching(collection, new MatchingP(params));
	}
	
	/**
	 * Deletes the entire collection and all of its objects.
	 * 
	 * @param collection
	 * @return
	 */
	public boolean deleteCollection(String collection) {

		checkRegex(collection);

		File toDelete = new File(rootDirectory,collection);
		
		return FileSystemUtils.deleteRecursively(toDelete);
		
	}
	
	/**
	 * Iterates the collection, passing each item to the specified consumer.
	 * It is up to the consumer to decide what to the do with it.
	 * 
	 * @param collection
	 * @param c
	 */
	public void readAll(String collection, Consumer<JsonNode> c){
		readAllFiles(collection, rowFile -> {
			try {
				c.accept( objectMapper.readTree(rowFile) );
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Iterates the collection, deserialzing each item as the specified
	 * Java class and passing it to the specified consumer.
	 * It is up to the consumer to decide what to the do with it.
	 * 
	 * @param collection
	 * @param c
	 */
	public <T> void readAll(String collection, Class<T> clazz, Consumer<T> c){
		readAllFiles(collection, rowFile -> {
			try {
				c.accept( objectMapper.readValue(rowFile,clazz) );
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	
	public void archive(String collection) {
		archive(collection,null);
	}
	
	/**
	 * Moves the current contents of the collection to an "archive" location,
	 * notionally like a "recycle bin".
	 * 
	 * @param collection
	 */
	public void archive(String collection, Predicate<JsonNode> predicate) {
		
		File collectionDirecory = directoryExists(collection);
		
		File archiveDirectory = new File(collectionDirecory,archiveName);

		if(!archiveDirectory.exists()) {
			archiveDirectory.mkdirs();
        }		
		
		readAllFiles(collection, f -> {
			
			try {
				if(predicate == null || predicate.test(objectMapper.readTree(f))) {
					
					File destination = new File(archiveDirectory, f.getName());
					
					Files.move(Path.of(f.toURI()), Path.of(destination.toURI()));
					
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			
		});
		
		
	}
	
	/*
	 * "Magic" constants
	 */
	private final static String ID = "id";
	private final static String DOT_JSON = ".json";
	private final static String REGEX="^[A-Za-z0-9_.-]+";

	private String generateId() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
	
	private File directoryExists(String collection) {
		
		checkRegex(collection);
		
		File collectionDir = new File(rootDirectory,collection);
		
		if(!collectionDir.exists() ) {
			collectionDir.mkdirs();
		}
		return collectionDir;
	}
	
	private void readAllFiles(String collection, Consumer<File> consumer ) {

		File collectionDir = directoryExists(collection);
		
		ArrayNode list = objectMapper.createArrayNode();
		
		for( File rowFile : collectionDir.listFiles( (dir,name) -> name.endsWith(DOT_JSON))) {

			consumer.accept(rowFile);
		
		}
		
	}
	

	private void checkRegex(String collection) {
		if( !collection.matches(REGEX) ) {
			throw new IllegalArgumentException("Collection and ID names must match " + REGEX);
		}
	}


	
	private static class MatchingP implements Predicate<JsonNode> {

		JsonNode params;
		
		public MatchingP(JsonNode params) {
			this.params = params;
		}

		@Override
		public boolean test(JsonNode row) {
			Iterator<Entry<String, JsonNode>> i = params.fields();
			
			while(i.hasNext() ) {
				Entry<String, JsonNode> e = i.next();
				
				if( !row.has(e.getKey()) || !row.get(e.getKey()).equals( e.getValue() )) {
					return false;
				}
				
				return true;
			}
			
			return true;
		}
		
	}
	
	
	
}
