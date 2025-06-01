### Requirements
- Java 17

## Usage
After the server is up you can use this json to import to your postman and call the apis
https://api.postman.com/collections/8845584-c7374425-c2fa-4603-aa2b-cd11367f2d10?access_key=PMAT-01JWNMWFVXKTSP9VJZ4DAQXEXH

### Database
- We're using embedded H2 database.
- Connect via http://localhost:8080/h2-console/
- Db Address is "jdbc:h2:mem:testdb"
- Username is "sa"
- Password is "password"
- One admin and two users are automatically created by [data.sql](src/main/resources/data.sql)
- Because we combine manual id assigning in data.sql with sequence based assignment, requests may fail in the first 
try if the application was not shut down properly. Try sending the request again if the first one fails.
