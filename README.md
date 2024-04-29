overview of "Mini-Bank" project:

Technology Stack:
1. Akka Typed Actor
2. Akka Persistence
3. Akka Http
4. Cassandra
5. Cats

Requirments:
1. Basic Akka and Cats
2. Docker

Features:
1. CRUD on bank account
2. Update balance of account 
3. and so on....

Basic Points:
1. Each bank account is a persistant actor
2. All events are recorded(create, update e.t.c)
3. All events are replayed, in case of failure/restart
4. One BigBank(persistant) actor manage all actor
5. A Http Server with a rest api handles request
6. All events are stored in cassandra

Apis:
1. POST: will create a bank account and store data at bank-accounts/UUID 
   1.1: /
   1.2: body json payload
   1.3: response status 201 (created) 400 (Bad request)
   
curl -v -X POST http://localhost:8080/bank-accounts\
   -H 'Content-Type: application/json'\
   -d '{"user":"rcardin", "currency":"EUR", "balance": 1000.0}'
   
2. GET: retrieve the current details of bank account
   2.1: /uuid (uuid is a unique identifier of bank account)
   2.2: response status 200 (with retrieved bank account) and 404 (Not Found)
   
curl -v http://localhost:8080/bank-accounts/ce1f4ac3-f1be-4523-b323-25e81d90322f
   
3. PUT: update the account details
   3.1: /uuid
   3.2: body json payload
   3.3: response 200(with updated bank account), 400 (Bad Request), 404 (Not Found)
   
curl -v -X PUT http://localhost:8080/bank-accounts/5e36bcd7-dd7d-43d6-90f0-de08cd9f551d\
   -H 'Content-Type: application/json'\
   -d '{"currency":"EUR", "amount": 500.0}'
