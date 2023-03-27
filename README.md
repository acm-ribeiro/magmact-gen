# MagmaCt-Gen #

MagmaCt-Gen automatically generates an MagmaCt specification based on an existing OpenAPI Specification (OAS) document.
The generated specification is an extension of the original OAS JSON file. 

Currently, MagmaCt-Gen is not able to generate invariants. As such, we provide a useful catalog with the most common templates for 
the user to complement the generated specification. 

The generated JSON file can then be used by Magma - a tool to automate the microservice testing process. 

1. [MagmaCt](#magma-ct)
2. [Running Example: Tournaments Management Application](#example)
   1. [Catalog](#catalog)
      1. [Automatically generated properties](#auto-generated)
         - [POST](#post)
         - [GET](#get)
         - [PUT](#put)
         - [DELETE](#delete)
      2. [Integrity Constraints](#integrity-constraints)
         - [Domain Constraints](#domain)
         - [Entity Integrity Constraints](#entity-integrity)
         - [Referential Integrity Constraints](#ref-integrity)
         - [Key Constraints](#key)
      3. [Numerical Invariants](#numerical)
3. [Generated File Structure](#file-structure)
4. [Installation](#installation)
5. [Usage](#usage)
6. [Support](#support)

# MagmaCt <a name="magma-ct"></a>
MagmaCt is a specification language to complement APIs' specifications based on first-order logic. Its purpose is to 
extend the OAS with useful properties for test generation, transforming these documents 
into useful testing artifacts. Apart from providing information for microservice testing, MagmaCt also provides 
an API with semantic.

MagmaCt's main feature is to logical predicates based on pure - i.e. without side-effects (GETs) - API 
operations. These predicates are used to write operation contracts. In the same way, MagmaCt is also used to write API 
invariants. Although being initially **designed for extending OAS**, MagmaCt can also be used with any API specification 
language that can be extended.

The grammar file can be consulted in the `src/main/magmact-grammar` directory. 

# Running Example: Tournaments Management Application <a name="example"></a>
We'll use this example to illustrate the catalog's properties. 

This application maintains information about `players` and `tournaments`. Players compete in tournaments, for which they can
be enrolled and disenrolled. Each tournament has a maximum capacity.

This application has two APIs: one responsible for managing `players`, and another for managing `tournaments` resources. The APIs 
are the following: 

```
Tournaments' API: 
```
   /tournaments/
      GET      - returns all tournaments 
      POST     - adds a new tournament
   /tournaments/{tournamentId}/
      GET      - returns the tournament with the given `tournamentId`
      PUT      - updated the tournament with the given `tournamentId`
      DELETE   - deletes the tournament with the given `tournamentId`
   
   /tournaments/{tournamentId}/capacity/
      GET      - returns the capacity of the tournament with the given `tournamentId`
   
   /tournaments/{tournamentId}/enrolments/
      POST     - enrols a player in the tournament with the given `tournamentId`
      GET      - returns the players enroled in the tournament with the given `tournamentId`
 
   /tournaments/{tournamentId}/enrollments/{playerNIF}/
      GET      - checks if the player with the given `playerNIF` is enroled in the tournament with the given `tournamentId`
      DELETE   - deletes the enrolment of the player with the given `playerNIF` in the tournament with the given `tournamentId`
```   
Players' API:
```
   /players/
      GET      - returns all players 
      POST     - adds a new player
   
   /players/{playerNIF}/
      GET      - returns the player with the given `playerNIF`
      PUT      - updates the player with the given `playerNIF`
      DELETE   - deletes the player with the given `playerNIF`
   
   /players/{playerNIF}/enrollments/
      GET      - returns the tournaments in which the player with the given `playerNIF` is enroled
```

💡 The full OAS of this application can be found in the `scr/main/examples` directory. 

## Catalog <a name="catalog"></a>

This catalog is meant to be used as a tool to enrich the automatically generated MagmaCt specification. We first 
introduce what will be automatically generated. Then, we show how to write a set of predicates, or invariants, that can be adapted to the users' needs. The predicates will be illustrated with the help of the previously presented running example. 


### Automatically Generated Properties <a name="auto-generated"></a>
These are the predicates our generator will produce. In certain cases, these predicates may not be exactly what the user
needs to specify so, please, **always check the produced results** and adapt them according to your needs. 


#### POST <a name="post"></a>
In a POST operation we would like to specify that the resource we're trying to insert does not exist and, after the 
insertion, the resource exists and what was inserted is exactly what we intended to. 

```
   Requires:
      response_code(GET /players/{playerID}) == 404

   Ensures:
      response_code(GET /players/{playerID}) == 200
      response_body(this) == request_body(this)
```

#### GET <a name="get"></a>
To use them in MagmaCt specifications, we assume GET operations are pure, meaning the system state remains 
unaltered. As such, their specification is as permissible as possible. 

```
   Requires:
      T

   Ensures:
      T
```

#### PUT <a name="put"></a>
In a PUT operation we would like to specify that the resource we're trying to update exists and, after the
insertion, the resource still exists and the stored data was exactly what we intended to change.

```
   Requires:
      response_code(GET /players/{playerID}) == 200

   Ensures:
      response_code(GET /players/{playerID}) == 200
      response_body(this) == request_body(this)
```

#### DELETE <a name="delete"></a>
In a DELETE operation we would like to specify that the resource we're trying to remove exists and that, after the
removal, the resource does not exist and that what was removed was exactly what we intended to.

```
   Requires:
      response_code(GET /players/{playerID}) == 200

   Ensures:
      response_code(GET /players/{playerID}) == 404
      response_body(this) == previous(response_body(GET/players/{playerID}))
```

### Integrity Constraints <a name="integrity-constraints"></a>
Integrity constraints in Database Management Systems (DBMS) are a set of rules that are applied on the table columns or 
relationships to ensure that the overall validity, integrity, and consistency (i.e. the quality) of the data present in 
the database table is maintained. All the constraints present in this section **are meant to be used as invariants** 
unless stated otherwise. 

#### Domain Constraints <a name="oas_custom_parser.domain"></a>
🔷 Can be defined as a valid set of values for an attribute (e.g. integer attributes cannot have string values)

These constraints are already enforced by the specification without the need for MagmaCt. For instance, when we’re trying
to define an integer value, OAS gives us the option to specify an integer format - for instance `int64`, `int32` -, 
`minimum` and `maximum` values, etc.

#### Entity Integrity Constraints <a name="entity-integrity"></a>
🔷 States that the primary key value cannot be null. We can use this for the ID’s.

OpenAPI 3.0 does not have an explicit `null` type. But we can use `nullable: true` to specify that the value may be 
`null`.  But it is often common we want to assert that an attribute value must not be `null`. In MagmaCt, it is done as 
follows:

`for p in response_body(GET /players) :-  p.playerID != null`

#### Referential Integrity Constraints <a name="ref-integrity"></a> 
🔷 Requires that a foreign key must have a matching primary key or it must be null. This constraint is specified 
between two tables (parent and child); it maintains the correspondence between rows in these tables. It means the 
reference from a row in one table to another table must be valid.

As of now, OAS has no means of enforcing referential integrity between two records — as it should. This is often the 
DBMS’ job to enforce these constraints. However, if we’re accessing a DB developed by a third party, and we want to make 
sure this is enforced, we might want to define an MagmaCt predicate. As such, let's assume that each player has a `score`.
And this `score` is stored in a different table for efficiency purposes. A referential integrity constraint might be that
every table entry must belong to an existing player:

`for s in response_body(GET /scores): exists p in response_body(GET /players):- s.playerID == p.playerID`

#### Key Constraints <a name="key"></a> 
🔷 Guarantees the uniqueness of primary keys

Although we can specify that array items must be unique, we do not have an explicit way to do this in OAS for other 
values, such as entity IDs.

`for p1, p2 in response_body(GET /players) :- p1.playerID != p2.playerID`

Alternatively, we can write this as a post condition for the `GET /players/{playerID}` operation with the help of the 
`length` function: 

`response_body(GET /player/{playerID}.length == 1`

### Numerical Invariants <a name="numerical"></a>
🔷 Numeric constraints refer to the application's numeric properties and set lower or upper bounds to data values.

Using our example, we can say that the number of enrolled players on a tournament cannot exceed the tournament's 
capacity. 

`for t in response_body(GET /tournaments) :- 
   response_body(GET /tournaments/{t.tournamentId}/enrollments).length <= 
   response_body(GET /tournaments/{t.tournamentId}/capacity)`


## Generated File Structure <a name="file-structure"></a>
The generated file will be a standard `.json` file. You can find a full example of an MagmaCt-Gen output in the 
`src/main/examples` directory. The file `apostl_spec.json` is the output of executing MagmaCt-Gen with the 
`tournaments.json` specification. This file is a standard OAS. The file `tournaments_extended.json` 
is a complete specification for the tournament's application, extended with a valid and complete MagmaCt specification. 

```json
{
   "servers": [], 
   "invariants": [], 
   "paths": {
      "uri": {
         "GET | POST | PUT | DELETE": {
            "operationID": "", 
            "requires": [], 
            "ensures": [], 
            "parameters": [
               {
                  "name": "", 
                  "in": "", 
                  "required": "", 
                  "schema": {}
               }               
            ],
            "requestBody": {}
         }
      }
   },
   "schemas": []
}
```

## Installation <a name="installation"></a>
MagmaCt-Gen implementation is within a `.jar` file so there is no installation required. 
Requires Java 16 or higher installed. 


## Usage  <a name="usage"></a>
In order to execute the MagmaCt-Gen `.jar` file, you can do the following: 

``java -jar magmact-gen.jar <path_to_oas_json>``

The generated json specification will be stored in the `.jar` location. 

---
### Support <a name="support"></a>
For any questions related to the Magma framework please contact 
[acm.ribeiro@campus.fct.unl.pt](mailto:acm.ribeiro@campus.fct.unl.pt)
