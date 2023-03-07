package com.adaptershack.jeffdb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class Controller {
	
	@Autowired
	DatabaseService db;

	@RequestMapping(value="/{collection}/{id}",method=RequestMethod.GET,produces="application/json")	
	ResponseEntity<JsonNode> get(@PathVariable String collection, @PathVariable String id) {
		
		JsonNode row = db.get(collection, id);
		
		if(row != null ) {
			return ResponseEntity.ok(row);
		} else {
			return ResponseEntity.notFound().build();
		}
		
	}

	@RequestMapping(value="/{collection}",method=RequestMethod.POST,consumes="application/json",produces="application/json")	
	public JsonNode insert(@PathVariable String collection, @RequestBody ObjectNode row) {
		return db.insert(collection, row);
	}
	
	@RequestMapping(value="/{collection}",method=RequestMethod.GET,produces="application/json")	
	public JsonNode listAll(@PathVariable String collection) {
		return db.listAll(collection);
	}
	
	@RequestMapping(value="/{collection}/{id}",method=RequestMethod.DELETE,produces="application/json")	
	public ResponseEntity<JsonNode> delete(@PathVariable String collection, @PathVariable String id) {
	
		if( db.delete(collection, id)) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();			
		}
	}

	
	@RequestMapping(value="/{collection}/{id}",method=RequestMethod.PUT,produces="application/json",consumes="application/json")	
	public JsonNode put(@PathVariable String collection, @PathVariable String id, @RequestBody ObjectNode row) {
		return db.update(collection, id, row);
	}
		
	@RequestMapping(value="/{collection}/search",method=RequestMethod.POST,consumes="application/json",produces="application/json")	
	public JsonNode postSearch(@PathVariable String collection, @RequestBody ObjectNode params) {

		return db.listMatching(collection,params);
		
	}

	@RequestMapping(value="/{collection}/search",method=RequestMethod.DELETE,produces="application/json")	
	public ResponseEntity<JsonNode> deleteSearch(@PathVariable String collection, @RequestBody ObjectNode params) {

		if( db.deleteMatching(collection,params)) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();
		}
		
	}
	
	@RequestMapping(value="/{collection}",method=RequestMethod.DELETE,produces="application/json")	
	public ResponseEntity<JsonNode> deleteCollection(@PathVariable String collection) {

		if( db.deleteCollection(collection)) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();
		}
		
	}
	
	
	
}
