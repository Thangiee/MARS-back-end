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
    
    **Both username and password need to be base 64 encoded!**
  
  * Get a Session (preferably):
    
    Use the above method to [get a session cookie](#session-login), afterwards, you just send the cookie instead the 
    credentials for future API calls.


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
* [Change Instructor Account Admin](#change-instructor-account-admin)
* [Change Account Password](#change-account-password)
* [Approve Account](#approve-account)
* [Account Deletion](#account-deletion)
* [Clock In](#clock-in)
* [Clock Out](#clock-out)
* [Clock In/Out Record Info](#clock-inout-record-info)
* [Record Deletion](#record-deletion)
* [Update Clock In/Out Record](#update-clock-inout-record)
* [Facial Recognition](#facial-recognition)
* [Add Face For Recognition](#add-face-for-recognition)
* [Remove Face From Recognition](#remove-face-from-recognition)
* [Get Face Images Info](#get-face-images-info)
* [Register UUID](#register-uuid)
* [Verify Registered UUID](#verify-registered-uuid)
* [Session Login](#session-login)
* [Session Logout](#session-logout)
* [Email Time-sheet](#email-time-sheet)

--

#### Account Info

Get info of the current account or specify {*netid*} to get info about a specific account.

To get multiple specific accounts by specifying {*netids*}, which are net ids separated by comma. <br/>
Example: `http://52.33.35.165:8080/api/account/?net-ids=demo123,aaa123`

| Method   | Route                           | Authorized                    |
|:--------:|---------------------------------|-------------------------------|
| GET      | /account                        | Admin, Instructor, Assistant  |
| GET      | /account/all                    | Admin                         |
| GET      | /account/?net-ids={*netids*}    | Admin                         |
| GET      | /account/{*netid*}           | Admin                         |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [account](#account)            |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

### Assistant Info

Get info of the current assistant or specify {*netid*} to get info about a specific assistant.

To get multiple specific assistants info by specifying {*netids*}, which are net ids separated by comma. <br/>
Example: `http://52.33.35.165:8080/api/assistant/?net-ids=demo123,aaa123`

| Method   | Route                           | Authorized         |
|:--------:|---------------------------------|--------------------|
| GET      | /assistant                      | Assistant          |
| GET      | /assistant/all                  | Admin, Instructor  |
| GET      | /assistant/?net-ids={*netids*}  | Admin, Instructor  |
| GET      | /assistant/{*netid*}            | Admin, Instructor  |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [Assistant](#assistant)        |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
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
| net_id    | String | yes      | The UT Arlington netID    |
| user      | String | yes      | The account username      |
| pass      | String | yes      | The account password      |
| email     | String | yes      | The assistant email       |
| rate      | Double | yes      | Dollar per hour           |
| job       | String | yes      | The assistant job         |
| dept      | String | yes      | The assistant department  |
| first     | String | yes      | The assistant first name  |
| last      | String | yes      | The assistant last name   |
| emp_id    | String | yes      | The assistant employee ID |
| threshold | Double | optional | Can use to determine pass or fail for this assistant facial recognition result. Value must be between 0 and 1. Default is 0.4 if a value is not provided. |
| title     | String | yes      | The assistant title       |
| title_code| String | yes      | The assistant titlecode   |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Assistant account created             |
|        400       | [Bad request](#400-bad-request)       |
|        409       | Conflict. Net Id or username already exists |
|        500       | [Internal Error](#500-internal-error) |

---

#### Update Assistant

Update an assistant info.

| Method      | Route              | Authorized                    |
|:-----------:|--------------------|-------------------------------|
| POST or PUT | /assistant         | Assistant                     |
| POST or PUT | /assistant/{*netId*}| Admin, Instructor            |

Parameters

| Key       | Type   | Required | Description               |
|-----------|--------|----------|---------------------------|
| emp_id    | String | yes      | The assistant employee ID |
| rate      | Double | optional | Dollar per hour           |
| job       | String | optional | 'teaching' or 'grading'   |
| dept      | String | optional | The assistant department  |
| title     | String | optional | The assistant title       |
| title_code| String | optional | The assistant titlecode   |
| threshold | Double | optional | **Only admin and instructor can set this field.** <br> Can use to determine pass or fail for this assistant facial recognition result. Value must be between 0 and 1. 

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Assistant updated                     |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

### Instructor Info

Get info of the current instructor or specify {*netid*} to get info about a specific instructor.

To get multiple specific instructors info by specifying {*netids*}, which are net ids separated by comma. <br/>
Example: `http://52.33.35.165:8080/api/instructor/?net-ids=demo123,aaa123`

| Method   | Route                          | Authorized                    |
|:--------:|--------------------------------|-------------------------------|
| GET      | /instructor                    | Admin, Instructor             |
| GET      | /instructor/all                | Admin                         |
| GET      | /instructor/?net-ids={*netids*}| Admin                         |
| GET      | /instructor/{*netid*}          | Admin                         |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [Instructor](#instructor)      |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
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
| net_id    | String | yes      | The UT Arlington netID    |
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
|        409       | Conflict. Net Id or username already exists |
|        500       | [Internal Error](#500-internal-error) |

---

#### Update Instructor

Update an instructor info.

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST or PUT | /instructor         | Admin, Instructor             |

Parameters

| Key       | Type   | Required | Description               |
|-----------|--------|----------|---------------------------|
| email     | String | optional | The assistant email       |
| first_name| String | optional | The assistant first name  |
| last_name | String | optional | The assistant last name   |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Instructor updated                    |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Change Instructor Account Admin 

Change the route of an instructor account between an admin or a regular instructor.
**Note** that all admins are instructor but not all instructors are admin.

| Method      | Route                                        | Authorized                    |
|:-----------:|----------------------------------------------|-------------------------------|
| POST or PUT | /account/instructor/change-role/{*netId*}    | Admin                         |

Parameters

| Key         | Type   | Required | Description                                                          |
|-------------|--------|----------|----------------------------------------------------------------------|
| is_admin    | boolean| yes      | True to change role to "admin" else role will be set as "instructor" |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return updated [account](#account)    |
|        400       | [Bad request](#400-bad-request)       |
|        400       | Require an instructor account         |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Change Account Password

Change the password of the current account or specify {*netid*} to do it for a specific account.

| Method      | Route                                 | Authorized                    |
|:-----------:|---------------------------------------|-------------------------------|
| POST or PUT | /account/change-password              | Admin, Instructor, Assistant  |
| POST or PUT | /account/change-password/{*netid*} | Admin                         |

Parameters

| Key         | Type   | Required | Description               |
|-------------|--------|----------|---------------------------|
| new_password| String | yes      | The account new password  |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Account password changed              |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Approve Account 

Approve or un-approve a specific account.

| Method      | Route                                 | Authorized                    |
|:-----------:|---------------------------------------|-------------------------------|
| POST or PUT | /account/change-approve/{*netid*}  | Admin                         |

Parameters

| Key         | Type   | Required | Description               |
|-------------|--------|----------|---------------------------|
| approve     | Boolean| yes      | Account need to be approved to have access to resources |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Account approve changed               |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Account Deletion

Delete a specific account. This action will cascade and **DELETE ALL** other data relating to the account.

| Method      | Route                 | Authorized                    |
|:-----------:|-----------------------|-------------------------------|
| DELETE      | /account/{*netid*} | Admin                         |

Returning 

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Account deleted                       |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
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
| computer_id | String | optional | The computer id used register the UUID |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Clock in success                      |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        409       | Conflict. Assistant is already clocked in |
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
| computer_id | String | optional | The computer id used to register the UUID |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Clock out success                     |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        409       | Conflict. Assistant is already clocked out |
|        500       | [Internal Error](#500-internal-error) |

---

#### Clock In/Out Record Info

Get all clock in/out records for the current assistant or specify {*netid*} to get it for a specific assistant.

filter {*option*}:
  * `pay-period` - get records only from the current pay period
  * `month` - get records only from the current month
  * `year`  - get records only from the current year

| Method      | Route                                  | Authorized                    |
|:-----------:|----------------------------------------|-------------------------------|
| GET         | /records                               | Assistant                     |
| GET         | /records?filter={*option*}             | Assistant                     |
| GET         | /records/{*netid*}                     | Admin, Instructor             |
| GET         | /records/{*netid*}?filter={*option*}   | Admin, Instructor             |
| GET         | /records/all                           | Admin, Instructor             |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [ClockInOutRecord](#clockinoutrecord) |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Update Clock In/Out Record

Update a specific record. 

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| POST or PUT | /records/{*id*}      | Admin, Instructor             |

Parameters

| Key         | type   | Required | Description                                | 
|-------------|--------|----------|--------------------------------------------| 
| in_time     | long   | yes      | Clock in time, epoch time in milliseconds  | 
| out_time    | long   | yes      | Clock out time, epoch time in milliseconds | 
| in_comp_id  | string | optional | The computer id used to register the UUID  | 
| out_comp_id | string | optional | The computer id used to register the UUID  | 

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Record updated                        |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Record Deletion

Delete a specific clock in/out record by its {*id*}.

| Method      | Route                 | Authorized                    |
|:-----------:|-----------------------|-------------------------------|
| DELETE      | /records/{*id*}       | Admin, Instructor             |

Returning 

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | record deleted                        |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Facial Recognition 

Given a face image, calculate the confidence that face belongs to the same assistant.

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| POST or PUT | /face/recognition    | Assistant                     |

Parameters

| Key       | type       | Required | Description                                | 
|-----------|------------|----------|--------------------------------------------| 
| img       | byte array | yes      | The binary data of the face image to check |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [Recognition Result](#recognition-result) |
|        400       | [Bad request](#400-bad-request)       |
|        400       | Can't find a face on the uploaded image |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        413       | The included image is too large       |
|        500       | [Internal Error](#500-internal-error) |

---

#### Add Face For Recognition

Add a face to the assistant to be used for calculating recognition results.

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| POST        | /face                | Assistant                     |

Parameters

| Key       | type       | Required | Description                                | 
|-----------|------------|----------|--------------------------------------------| 
| img       | byte array | yes      | The binary data of the face image          |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [Image URL](#url)              |
|        400       | [Bad request](#400-bad-request)       |
|        400       | Can't find a face on the uploaded image |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        413       | The included image is too large       |
|        500       | [Internal Error](#500-internal-error) |

---

#### Remove Face From Recognition

Remove a face image from being used for calculating recognition results. 

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| DELETE      | /face/{*imageId*}    | Admin, Instructor             |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Image removed                         | 
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Get Face images Info

Get all face images info of the current assistant or specify {*netid*} to get them
for a specific assistant.

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| GET         | /face                | Assistant                     |
| GET         | /face/{*netid*}      | Admin, Instructor             |

Returning

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Return [Image Info](#image-info)      |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

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
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Verify Registered UUID

Verify that a specific {*uuid*} is still registered (use in the clock in/out process)

| Method      | Route              | 
|:-----------:|--------------------|
| GET         | /register-uuid/verify/{*uuid*} |

Returning 

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | The UUID is valid at the time of the request. Return the given UUID. |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        410       | Gone. Either the UUID was not registered or it has been expired |
|        500       | [Internal Error](#500-internal-error) |

---

#### Session Login

Start a session. The server will generate a authentication cookie and give it to the client for identifying the session.
Said cookies is in the Http response header. 

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST or PUT | /session/login      | Admin, Instructor, Assistant  |

Returning 

| HTTP Status Code | Description                           |
|:----------------:|---------------------------------------|
|        200       | Successfully login to a session. Also return the [account info](#account). |
|        400       | [Bad request](#400-bad-request)       |
|        401       | [Unauthorized](#401-unauthorized)     |
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
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        500       | [Internal Error](#500-internal-error) |

---

#### Email Time-sheet

Generate and email a time-sheet for the first half month(1-15) or second half month(16 to last day of the month)
of a specific {*year*} and {*month*} for the current assistant or specify {*netid*} for a specific assistant. 
E-mail many take up to a few minutes to arrive.

Example: 

Time-sheet for pay period 9/1/2015 - 9/15/2015  `/time-sheet/first-half-month?year=2015&month=9`	
Time-sheet for pay period 9/16/2015 - 9/30/2015  `/time-sheet/second-half-month?year=2015&month=9`	

| Method      | Route                                                                 | Authorized |
|:-----------:|-----------------------------------------------------------------------|------------|
| GET         | /time-sheet/first-half-month?year={*year*}&month={*month*}            | Assistant  |
| GET         | /time-sheet/second-half-month?year={*year*}&month={*month*}           | Assistant  |
| GET         | /time-sheet/{*netid*}/first-half-month?year={*year*}&month={*month*}  | Admin, Instructor |
| GET         | /time-sheet/{*netid*}/second-half-month?year={*year*}&month={*month*} | Admin, Instructor |

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
|        401       | [Unauthorized](#401-unauthorized)     |
|        403       | [Forbidden](#403-forbidden)           |
|        404       | [Not Found](#404-not-found)           |
|        500       | [Internal Error](#500-internal-error) |

---

Data Models
-----------
Data encoded in JSON that some APIs will return on an HTTP 200.

#### Account 

**Note** that all admins are instructor but not all instructors are admin.

| Key           | Type   | Description                                             |
|:-------------:|--------|---------------------------------------------------------|
| approve       | Boolean| Account need to be approved to have access to resources |
| netID         | String | The UT Arlington netID                                  |
| role          | String | The account role (admin, instructor, or assistant)      |
| username      | String | The account username                                    |
| createTime    | Long   | When the account was created, epoch time in milliseconds|


```json
// example json response
{
    "approve": true,
    "netId": "tql7155",
    "role": "assistant",
    "username": "thangiee",
    "createTime": 1451077341962
}
```

```json
// example json response for multiple accounts
{
  "accounts": [
    {
      "approve": false,
      "netId": "demo123",
      "role": "assistant",
      "username": "demo_asst",
      "createTime": 1451762754206
    }, 
    {
      "approve": true,
      "netId": "abc123",
      "role": "instructor",
      "username": "Ewing",
      "createTime": 1451583070262
    },
    {
      "approve": true,
      "netId": "mw002",
      "role": "assistant",
      "username": "test_minglu",
      "createTime": 1454347847628
    }
  ]
}
```

#### Assistant

| Key        | Type   | Description               |
|------------|--------|---------------------------|
| rate       | Double | Dollar per hour           |
| approve    | Boolean| Account need to be approved to have access to resources |
| netId      | String | The UT Arlington netID    |
| email      | String | The assistant email       |
| role       | String | The account role (admin, instructor, or assistant)      |
| username   | String | The account username                                    |
| createTime | Long   | When the account was created, epoch time in milliseconds|
| job        | String | The assistant job         |
| department | String | The assistant department  |
| firstName  | String | The assistant first name  |
| lastName   | String | The assistant last name   |
| employeeId | String | The assistant employee ID |
| threshold  | Double | Can use to determine pass or fail for a facial recognition result. |
| title      | String | The assistant title       |
| titleCode  | String | The assistant titlecode   |

```json
// example json response
{
  "rate": 10.4,
  "approve": false,
  "netId": "aaa123",
  "email": "aaa@mavs.uta.edu",
  "role": "assistant",
  "username": "demo_grader",
  "createTime": 1453571578410,
  "job": "grading",
  "department": "CSE",
  "lastName": "grader",
  "firstName": "test",
  "employeeId": "10001234560",
  "threshold": 0.4,
  "title": "some title",
  "titleCode": "123"
}
```

```json
/// example json response for multiple assistants
{
  "assistants": [
    {
      "rate": 10.4,
      "approve": false,
      "netId": "aaa123",
      "email": "aaa@mavs.uta.edu",
      "role": "assistant",
      "username": "demo_grader",
      "createTime": 1453571578410,
      "job": "grading",
      "department": "CSE",
      "lastName": "grader",
      "firstName": "test",
      "employeeId": "10001234560",
      "threshold": 0.4,
      "title": "some title",
      "titleCode": "123"
    },
    {
      "rate": 10.4,
      "approve": true,
      "netId": "demo123",
      "email": "bbb0@gmail.com",
      "role": "assistant",
      "username": "demo_asst",
      "createTime": 1451762754206,
      "job": "teaching",
      "department": "CSE",
      "lastName": "user",
      "firstName": "test",
      "employeeId": "10001234567",
      "threshold": 0.4,
      "title": "some title",
      "titleCode": "123"
    }
  ]
}
```

#### Instructor

**Note** that all admins are instructor but not all instructors are admin.

| Key        | Type   | Description               |
|------------|--------|---------------------------|
| approve    | Boolean| Account need to be approved to have access to resources |
| netId      | String | The UT Arlington netID    |
| email      | String | The assistant email       |
| role       | String | The account role (admin, instructor, or assistant)      |
| username   | String | The account username                                    |
| createTime | Long   | When the account was created, epoch time in milliseconds|
| lastName   | String | The assistant last name   |
| firstName  | String | The assistant first name  |

```json
// example json response
{
  "approve": true,
  "netId": "abc123",
  "email": "eee@gmail.com",
  "role": "admin",
  "username": "Ewing",
  "createTime": 1451583070262,
  "lastName": "Ewing",
  "firstName": "D"
}
```

```json
// example json response for multiple instructors
{
  "instructors": [
    {
      "approve": true,
      "netId": "abc123",
      "email": "eee@gmail.com",
      "role": "admin",
      "username": "Ewing",
      "createTime": 1451583070262,
      "lastName": "Ewing",
      "firstName": "D"
    },
    {
      "approve": true,
      "netId": "abcd123",
      "email": "bbbb@gmail.com",
      "role": "instructor",
      "username": "bob123",
      "createTime": 1451583070262,
      "lastName": "B",
      "firstName": "Bob"
    }
  ]
}
```

#### ClockInOutRecord

| Key           | Type   | Description                                                                                  |
|---------------|--------|----------------------------------------------------------------------------------------------|
| inTime        | Long   | Clock in time, epoch time in milliseconds.                                                   |
| inComputerId  | String | The computer id used to register the UUID and generate the QR code, or **null** if not avaliable |
| netId         | String | The UT Arlington netID                                                                       |
| id            | Int    | Id for the record                                                                            |
| outTime       | Long   | Clock out time, epoch time in milliseconds, or **null** if this record has not been clock out.    |
| outComputerId | String | The computer id used to register the UUID and generate the QR code, or **null** if not avaliable |

```json
// example json response
{
  records: [
    {
      "inTime": 1441385100000,
      "inComputerId": "ERB 103",
      "netId": "tql7155",
      "id": 3,
      "outTime": 1441390500000,
      "outComputerId": null
    },
    {
      "inTime": 1472839200000,
      "inComputerId": "incomp",
      "netId": "demo123",
      "id": 2,
      "outTime": 1472842800000,
      "outComputerId": "outcomp"
    }
  ]
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

#### Recognition Result

| Key           | Type     | Description                                                                                  |
|---------------|----------|----------------------------------------------------------------------------------------------|
| confidence    | Double   | A value between 0 and 1 of the likelihood the given face belongs to the same person.
| threshold     | Double   | A value between 0 and 1 that can be compared with confidence to determine pass or fail recognition. Different assistants can have different values. 

```json
// example json response
{
  "confidence": 0.673156,
  "threshold": 0.4
}
```

#### URL 

| Key           | Type     | Description                                                                                  |
|---------------|----------|----------------------------------------------------------------------------------------------|
| url           | String   | Url to the image 

```json
// example json response
{
  "url":"http://localhost:8080/api/assets/face/Xb1yQze.jpg"
}
```

#### Image Info 

You can appending `size` parameter at the end of the url to have that image 
resized with its width and height equal to `size` pixels. `size` can have a value
anywhere between 3 to 512.

For example, the image located at `http://localhost:8080/api/assets/face/zwxNke.jpg`
can be resized to 64x64 px with `http://localhost:8080/api/assets/face/zwxNke.jpg?size=64`


| Key           | Type     | Description                                                                                  |
|---------------|----------|----------------------------------------------------------------------------------------------|
| id            | String   | Image id
| url           | String   | Url to the image 

```json
// example json response
{
  "images": [
    {
      "id": "zwxNke.jpg",
      "url": "http://localhost:8080/api/assets/face/zwxNke.jpg"
    },
    {
      "id": "Xb1yQze.jpg",
      "url": "http://localhost:8080/api/assets/face/Xb1yQze.jpg"
    }
  ]
}
```

Common HTTP Error Codes
-----------------------

#### 400 Bad Request

The request contains bad syntax or invalid parameters/form-data.

#### 401 Unauthorized

The account is not properly authenticated due to invalid username and/or password.

#### 403 Forbidden

You don't have access to this resource because:
  * the account has not be approve by the administrator.
  * the account role is not authorized. 

#### 404 Not Found

The requested resource that does not exist.

#### 500 Internal Error

Something when wrong on the server side.
