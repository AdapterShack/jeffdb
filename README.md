# JEFFDB: Jeff's Essentially Feature-Free Database

This is an unviably minimal implementation of a document oriented database.

Features (or lack thereof):

* Only JSON is supported.
* All data is stored as JSON files written directly to the filesystem. 
* Files are organized into "collections",ie., directories on the filesystem.
* Files are simply named after id + ".json" where "id" is either provided or autogenerated.
* Yes, the id field must literally be named "id".
* Getting an object by its ID is simply asking the server to read that one file by name.
* Getting an object by any other field, scans every file in the directory. In RDMS terms, every select is a table scan, and there are no indexes.
* No attempt ever to lock anything or be safe for concurrent uses - we assume only one user at a time (if even one).

You probably should not use this as a database. It was written mainly as an exercise in
developing an extremely small Spring Boot project.

This may be run as a standalone web app which provides a REST API, or embedded in another project as JAR file (only recommended if your data storage needs are truly minimal).

##REST API

Read from collection "foo":

```
curl -i -s http://localhost:8080/foo
```

Append to collection:

```
curl -i -s http://localhost:8080/foo -X POST -H 'content-type: application/json' -d '{"name":"jeff"}'
```

Get object by ID:

```
curl -i -s http://localhost:8080/foo/cef576a881b4419aaf63f0fc7d7a8ec8
```

Update by ID:

```
curl -i -s http://localhost:8080/foo/cef576a881b4419aaf63f0fc7d7a8ec8 -X PUT -H 'content-type: application/json' -d '{"name":"Jeff R."}'
```

Find by other field(s):

```
curl -i -s http://localhost:8080/foo/search -X POST -H 'content-type: application/json' -d '{"name":"Jeff R."}'
```

Delete by ID:

```
curl -i -s http://localhost:8080/foo/cef576a881b4419aaf63f0fc7d7a8ec8 -X DELETE
```

Delete by other fields:

```
curl -i -s http://localhost:8080/foo/search -X DELETE -H 'content-type: application/json' -d '{"name":"Jeff R."}'
```

Delete entire collection:

```
curl -i -s http://localhost:8080/foo -X DELETE
```

## As a library

All of the functions illustrated about can be done by calling `com.adaptershack.jeffdb.DatabaseService` directly from code.

```
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
```
