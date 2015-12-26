MARS-back-end
=============
The Mavs Assistant Reporting System (MARS) back-end server provides services that other 
systems can consume through its RESTful API.

Authentication
--------------
Most of the API endpoints require you to be authenticated. You can do that in one of two ways:

  * Using HTTP Basic Authentication: 
        
    Include the username and password in the URL on every API calls that require authentication.
    `http://username:password@example.com/resource`
  
  * Get a Session (preferably):
    
    Use the above method to [log into a session](#session-login), afterwards you will not need to 
    include your credentials for future API calls.


API Endpoints
-------------

*This API is still under development and is subject to changes.*

To use the APIs, you will need to form the full URL by combining the base URL with the targeted API's route.
The current base URL is `http://52.33.35.165:8080/api`.

Examples:
  * Get the current account info: `http://52.33.35.165:8080/api/account`
  * Get clock in/out records of a specific assistant: `http://52.33.35.165:8080/api/records/tql7155`

--

All Endpoints
  
* [Account Info](#account-info)
* [Assistant Info](#assistant-info)
* [Assistant Account Creation](#assistant-account-creation)
* [Update Assistant](#update-assistant)
* [Instructor Info](#instructor-info)
* [Instructor Account Creation](#instructor-account-creation)
* [Update Instructor](#update-instructor)
* [Change Account Password](#change-account-password)
* [Account Deletion](#account-deletion)
* [Clock In](#clock-in)
* [Clock Out](#clock-out)
* [Clock In/Out Record Info](#clock-inout-record-info)
* [Update Clock In/Out Record](#update-clock-inout-record)
* [Facial Recognition (WIP)](#facial-recognition-wip)
* [Register UUID](#register-uuid)
* [Session Login](#session-login)
* [Session Logout](#session-logout)
* [1st Half-month Time-sheet](#1st-half-month-time-sheet)
* [2nd Half-month Time-Sheet](#2nd-half-month-time-sheet)

--

#### Account Info

Get info of the current account or specify {*username*} to get info about a specific account.

| Method   | Route                | Authorized                    |
|:--------:|----------------------|-------------------------------|
| GET      | /account             | Admin, Instructor, Assistant  |
| GET      | /account/{*username*}| Admin                         |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [account](#account)            |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

### Assistant Info

Get info of the current assistant or specify {*netid*} to get info about a specific assistant.

| Method   | Route                  | Authorized                    |
|:--------:|------------------------|-------------------------------|
| GET      | /assistant             | Assistant                     |
| GET      | /assistant/{*netid*}   | Admin, Instructor             |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [Assistant](#assistant)        |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Assistant Account Creation

Create an assistant and setup an account for that assistant.

| Method   | Route              | 
|:--------:|--------------------| 
| POST     | /account/assistant | 

Parameters

| Key       | Type   | Required | Description               |
|-----------|--------|----------|---------------------------|
| netid     | String | yes      | The UT Arlington netID    |
| user      | String | yes      | The account username      |
| pass      | String | yes      | The account password      |
| email     | String | yes      | The assistant email       |
| rate      | Double | yes      | Dollar per hour           |
| job       | String | yes      | The assistant job         |
| dept      | String | yes      | The assistant department  |
| first     | String | yes      | The assistant first name  |
| last      | String | yes      | The assistant last name   |
| empid     | String | yes      | The assistant employee ID |
| title     | String | yes      | The assistant title       |
| titlecode | String | yes      | The assistant titlecode   |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Assistant account created             |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        409       | Conflict. Net Id or username already exists |
|        500       | [Internal Error](#500-internal-error) |

---

#### Update Assistant

Update an assistant info.

| Method      | Route              | Authorized                    |
|:-----------:|--------------------|-------------------------------|
| POST or PUT | /assistant         | Assistant                     |

Parameters

| Key       | Type   | Required | Description               |
|-----------|--------|----------|---------------------------|
| rate      | Double | optional | Dollar per hour           |
| dept      | String | optional | The assistant department  |
| title     | String | optional | The assistant title       |
| titlecode | String | optional | The assistant titlecode   |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Assistant updated                     |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

### Instructor Info

Get info of the current instructor or specify {*netid*} to get info about a specific instructor.

| Method   | Route                   | Authorized                    |
|:--------:|-------------------------|-------------------------------|
| GET      | /instructor             | Instructor                    |
| GET      | /instructor/{*netid*}   | Admin                         |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [Instructor](#instructor)      |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Instructor Account Creation

Create an instructor and setup an account for that instructor.

| Method   | Route               | 
|:--------:|---------------------| 
| POST     | /account/instructor | 

Parameter

| Key       | Type   | Required | Description               |
|-----------|--------|----------|---------------------------|
| netid     | String | yes      | The UT Arlington netID    |
| user      | String | yes      | The account username      |
| pass      | String | yes      | The account password      |
| email     | String | yes      | The instructor email      |
| first     | String | yes      | The instructor first name |
| last      | String | yes      | The instructor last name  |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Instructor account created            |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        409       | Conflict. Net Id or username already exists |
|        500       | [Internal Error](#500-internal-error) |

---

#### Update Instructor

Update an instructor info.

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST or PUT | /instructor         | Instructor                    |

Parameters

| Key       | Type   | Required | Description               |
|-----------|--------|----------|---------------------------|
| email     | String | optional | The assistant email       |
| firstname | String | optional | The assistant first name  |
| lastname  | String | optional | The assistant last name   |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Instructor updated                    |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Change Account Password

Change the password of the current account or specify {*username*} to do it for a specific account.

| Method      | Route                                 | Authorized                    |
|:-----------:|---------------------------------------|-------------------------------|
| POST or PUT | /account/change-password              | Admin, Instructor, Assistant  |
| POST or PUT | /account/change-password/{*username*} | Admin                         |

Parameters

| Key         | Type   | Required | Description               |
|-------------|--------|----------|---------------------------|
| newpassword | String | yes      | The account new password  |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Account password changed              |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Account Deletion

Delete a specific account. This action will cascade and **DELETE ALL** other data relating to the account.

| Method      | Route                 | Authorized                    |
|:-----------:|-----------------------|-------------------------------|
| DELETE      | /account/{*username*} | Admin                         |

Returning 

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Account deleted                       |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Clock In

Assistant clock in. You must be clock out before trying to clock in, otherwise, an error code will return.

| Method      | Route              | Authorized                    |
|:-----------:|--------------------|-------------------------------|
| POST        | /records/clock-in  | Assistant                     |

Parameters

| Key         | Type   | Required | Description                            |
|-------------|--------|----------|----------------------------------------|
| uuid        | String | yes      | The registered UUID                    |
| computerid  | String | optional | The computer id used register the UUID |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Clock in success                      |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        409       | Conflict. Assistant is already clocked in |
|        410       | Gone. Either the UUID was not registered or it has been expired |
|        500       | [Internal Error](#500-internal-error) |

---

#### Clock Out

Assistant clock out. You must be clock in before trying to clock out, otherwise, an error code will return.

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST        | /records/clock-out  | Assistant                     |

Parameters

| Key         | Type   | Required | Description                               |
|-------------|--------|----------|-------------------------------------------|
| uuid        | String | yes      | The registered UUID                       |
| computerid  | String | optional | The computer id used to register the UUID |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Clock out success                     |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        409       | Conflict. Assistant is already clocked out |
|        410       | Gone. Either the UUID was not registered or it has been expired |
|        500       | [Internal Error](#500-internal-error) |

---

#### Clock In/Out Record Info

Get all clock in/out records for the current assistant or specify {*netid*} to get it for a specific assistant.

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| GET         | /records             | Assistant                     |
| GET         | /records/{*netid*}   | Instructor                    |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [ClockInOutRecord](#clockinoutrecord) |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Update Clock In/Out Record

Update a specific record. 

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| POST or PUT | /records/{*id*}      | Instructor                    |

Parameters

| Key       | type   | Required | Discription                                | 
|-----------|--------|----------|--------------------------------------------| 
| intime    | long   | yes      | Clock in time, epoch time in milliseconds  | 
| outtime   | long   | yes      | Clock out time, epoch time in milliseconds | 
| incompid  | string | optional | The computer id used to register the UUID  | 
| outcompid | string | optional | The computer id used to register the UUID  | 

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Record updated                        |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Facial Recognition (WIP)

---

#### Register UUID

Register a short lived uuid. (use in the clock in/out process)

| Method      | Route              | 
|:-----------:|--------------------|
| POST        | /register-uuid     |

Parameters

| Key         | Type   | Required | Description                               |
|-------------|--------|----------|-------------------------------------------|
| uuid        | String | yes      | A valid UUID                              |

Returning 

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [UUID metadata](#uuid-metadata)|
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Session Login

Start a session. The server will generate a cookie and give it to the client for identifying the session. 
Most web browser will automatically set the cookie for you.

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST or PUT | /session/login      | Admin, Instructor, Assistant  |

Returning 

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Successfully login to a session       |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Session Logout

End a session.

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| POST or PUT | /session/logout      | Admin, Instructor, Assistant  |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Successfully log out of a session     |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### 1st Half-month Time-sheet

Generate and email a time-sheet for the first half month(1-15) of a specific {*year*} and {*month*} for  
the current assistant or specify {*netid*} for a specific assistant. E-mail many take up to a few minutes
to arrive.

Example: 

Time-sheet for pay period 9/1/2015 - 9/15/2015  `/time-sheet/first-half-month?year=2015&month=9`	

| Method      | Route                                                                 | Authorized |
|:-----------:|-----------------------------------------------------------------------|------------|
| GET         | /time-sheet/first-half-month?year={*year*}&month={*month*}            | Assistant  |
| GET         | /time-sheet/{*netid*}/first-half-month?year={*year*}&month={*month*}  | Instructor |

Parameters

| Key         | Type   | Required | Description                               |
|-------------|--------|----------|-------------------------------------------|
| year        | Int    | yes      | The year                                  |
| month       | Int    | yes      | The month [1..12]                         |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Successfully generated and emailed the time-sheet |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### 2nd Half-month Time-Sheet

Generate and email a time-sheet for the second half month(16 to last day of the month) of a 
specific {*year*} and {*month*} for the current assistant or specify {*netid*} for a specific assistant. 
E-mail many take up to a few minutes to arrive.

Example: 

Time-sheet for pay period 9/16/2015 - 9/30/2015  `/time-sheet/second-half-month?year=2015&month=9`	

| Method      | Route                                    | Authorized |
|:-----------:|------------------------------------------|------------|
| GET         | /time-sheet/second-half-month            | Assistant  |
| GET         | /time-sheet/{*netid*}/second-half-month  | Instructor |

Parameters

| Key         | Type   | Required | Description                               |
|-------------|--------|----------|-------------------------------------------|
| year        | Int    | yes      | The year                                  |
| month       | Int    | yes      | The month [1..12]                         |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Successfully generated and emailed the time-sheet |
|        400       | [Bad request](#400-bad-request)       |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

Data Models
-----------
Data encoded in JSON that some APIs will return on an HTTP 200.

#### Account 

| Key           | Type   | Description                                             |
|:-------------:|--------|---------------------------------------------------------|
| netID         | String | The UT Arlington netID                                  |
| role          | String | The account role (admin, instructor, or assistant)      |
| username      | String | The account username                                    |
| createTime    | Long   | When the account was created, epoch time in milliseconds|
| passwd        | String | This field will always be an empty string               |


```json
// example json response
{
    "netId": "tql7155",
    "role": "assistant",
    "username": "thangiee",
    "createTime": 1451077341962,
    "passwd": ""
}
```

#### Assistant

| Key        | Type   | Description               |
|------------|--------|---------------------------|
| rate       | Double | Dollar per hour           |
| netId      | String | The UT Arlington netID    |
| email      | String | The assistant email       |
| job        | String | The assistant job         |
| department | String | The assistant department  |
| firstName  | String | The assistant first name  |
| lastName   | String | The assistant last name   |
| employeeId | String | The assistant employee ID |
| title      | String | The assistant title       |
| titleCode  | String | The assistant titlecode   |

```json
// example json response
{
  "rate": 10.50,
  "netId": "tql7155",
  "email": "abc@gmail.com",
  "job": "grading",
  "department": "CSE",
  "lastName": "Smith",
  "firstName": "Bob",
  "employeeId": "123456789",
  "title": "some title",
  "titleCode": "some title code"
}
```

#### Instructor

| Key        | Type   | Description               |
|------------|--------|---------------------------|
| netId      | String | The UT Arlington netID    |
| email      | String | The assistant email       |
| firstName  | String | The assistant first name  |
| lastName   | String | The assistant last name   |

```json
// example json response
{
  "netId": "abc123",
  "email": "aaa@gmail.com",
  "lastName": "Ewing",
  "firstName": "David"
}
```

#### ClockInOutRecord

| Key           | Type   | Description                                                                                  |
|---------------|--------|----------------------------------------------------------------------------------------------|
| inTime        | Long   | Clock in time, epoch time in milliseconds.                                                   |
| inComputerId  | String | The computer id used to register the UUID and generate the QR code, or null if not avaliable |
| netId         | String | The UT Arlington netID                                                                       |
| id            | Int    | Id for the record                                                                            |
| outTime       | Long   | Clock in time, epoch time in milliseconds, or null if this record has not been clock out.    |
| outComputerId | String | The computer id used to register the UUID and generate the QR code, or null if not avaliable |

```json
// example json response
{
  "inTime": 1441385100000,
  "inComputerId": "ERB 103",
  "netId": "tql7155",
  "id": 3,
  "outTime": 1441390500000,
  "outComputerId": null
}
```

#### UUID metadata

| Key           | Type   | Description                                                                                  |
|---------------|--------|----------------------------------------------------------------------------------------------|
| ttl           | Long   | Time To Live. Milliseconds until the UUID expire                                             |
| expireTime    | Long   | The epoch time in milliseconds in which the UUID expire                                      |

```json
// example json response
{
  "ttl": 30000,
  "expireTime": 1451153243447
}
```

Common HTTP Error Codes
-----------------------

#### 400 Bad Request

The request contains bad syntax or invalid parameters/form-data.

#### 403 Forbidden

You don't have access to this resource because:
  * the account is not properly authenticated so check the username and password.
  * the account role is not authorized. 

#### 404 Not Found

The requested resource that does not exist.

#### 500 Internal Error

Something when wrong on the server side.