//Create user for security service in Mongo
db=db.getSiblingDB('admin')
db=db.getSiblingDB('authorization')
db.createUser({ user: "root",pwd: "iotdc3",roles: [ { role: "readWrite", db: "authorization" } ]});
