package com.adaptershack.jeffdb;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class DatabaseService {
	
	
	@Value("${com.adaptershack.jeffdb.root}")
	String rootDirectory;
	
	@Autowired
	ObjectMapper objectMapper;
	
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

	private void selfInit() {
		
		if(objectMapper == null) {
			objectMapper = new ObjectMapper();
		}
		
		if(rootDirectory==null) {
			rootDirectory="jeffdb";
		}
		
	}
	
	
	private final static String ID = "id";
	private final static String DOT_JSON = ".json";
	

	public ObjectNode insert(String collection, ObjectNode row) {

		selfInit();
		
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

	private String generateId() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	
	public ObjectNode update(String collection, String id, ObjectNode row) {

		row.put(ID, id);
		
		return insert(collection,row);
		
	}
	
	private final static String REGEX="^[A-Za-z0-9_.-]+";
	
	private File directoryExists(String collection) {
		
		checkRegex(collection);
		
		File collectionDir = new File(rootDirectory,collection);
		
		if(!collectionDir.exists() ) {
			collectionDir.mkdirs();
		}
		return collectionDir;
	}

	private void checkRegex(String collection) {
		if( !collection.matches(REGEX) ) {
			throw new IllegalArgumentException("Collection and ID names must match " + REGEX);
		}
	}

	public ObjectNode get(String collection, String id) {

		selfInit();

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
	
	public ArrayNode listAll(String collection) {
		return list(collection,null);
	}

	public ArrayNode listMatching(String collection,String key, String value) {
		return list(collection,
			(row) -> row.has(key) && row.get(key).asText().equals(value)
		);
	}
	
	
	public ArrayNode list(String collection, Predicate<JsonNode> predicate ) {

		selfInit();

		File collectionDir = directoryExists(collection);
		
		ArrayNode list = objectMapper.createArrayNode();
		
		for( File rowFile : collectionDir.listFiles( (dir,name) -> name.endsWith(DOT_JSON))) {
			try {
				JsonNode row = objectMapper.readTree(rowFile);
				if(predicate == null || predicate.test(row)) {
					list.add(row);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return list;
	}
	
	public boolean delete(String collection, String id) {
		
		File collectionDir = directoryExists(collection);

		File rowFile = new File( collectionDir, id + DOT_JSON );
		
		if(!rowFile.exists()) {
			return false;
		} else {
			return rowFile.delete();
		}
		
	}

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

	
	static class MatchingP implements Predicate<JsonNode> {

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
	
	
	public ArrayNode listMatching(String collection, JsonNode params) {
		return list(collection, new MatchingP(params));
	}
	
	public boolean deleteMatching(String collection, JsonNode params) {
		return deleteMatching(collection, new MatchingP(params));
	}
	
	public boolean deleteCollection(String collection) {

		checkRegex(collection);

		File toDelete = new File(rootDirectory,collection);
		
		return FileSystemUtils.deleteRecursively(toDelete);
		
	}
	
}
