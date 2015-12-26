
API Endpoints
=============

#### Account Info

| Method   | Route                | Authorized                    |
|:--------:|----------------------|-------------------------------|
| GET      | /account             | Admin, Instructor, Assistant  |
| GET      | /account/{*username*}| Admin, Instructor             |

---

#### Assistant Account Creation

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

---

#### Update Assistant

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

#### Instructor Account Creation

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

#### Update Instructor

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST or PUT | /instructor         | Instructor                    |

Parameters

| Key       | Type   | Required | Description               |
|-----------|--------|----------|---------------------------|
| email     | String | optional | The assistant email       |
| firstname | String | optional | The assistant first name  |
| lastname  | String | optional | The assistant last name   |

#### Change Account Password

| Method      | Route                                 | Authorized                    |
|:-----------:|---------------------------------------|-------------------------------|
| POST or PUT | /account/change-password              | Admin, Instructor, Assistant  |
| POST or PUT | /account/change-password/{*username*} | Admin                         |

Parameters

| Key         | Type   | Required | Description               |
|-------------|--------|----------|---------------------------|
| newpassword | String | yes      | The account new password  |

#### Account Deletion

| Method      | Route                 | Authorized                    |
|:-----------:|-----------------------|-------------------------------|
| DELETE      | /account/{*username*} | Admin                         |

#### Clock In

| Method      | Route              | Authorized                    |
|:-----------:|--------------------|-------------------------------|
| POST        | /records/clock-in  | Assistant                     |

Parameters

| Key         | Type   | Required | Description                            |
|-------------|--------|----------|----------------------------------------|
| uuid        | String | yes      | The registered UUID                    |
| computerid  | String | optional | The computer id used register the UUID |

#### Clock Out

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST        | /records/clock-out  | Assistant                     |

Parameters

| Key         | Type   | Required | Description                               |
|-------------|--------|----------|-------------------------------------------|
| uuid        | String | yes      | The registered UUID                       |
| computerid  | String | optional | The computer id used to register the UUID |

#### Clock In/Out Record Info

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| GET         | /records             | Assistant                     |
| GET         | /records/{*netid*}   | Instructor                    |

#### Update Clock In/Out Record

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

#### Facial Recognition (WIP)



#### Register UUID

| Method      | Route              | 
|:-----------:|--------------------|
| POST        | /register-uuid     |

Parameters

| Key         | Type   | Required | Description                               |
|-------------|--------|----------|-------------------------------------------|
| uuid        | String | yes      | A valid UUID                              |

#### Session Login

| Method      | Route               | Authorized                    |
|:-----------:|---------------------|-------------------------------|
| POST or PUT | /session/login      | Admin, Instructor, Assistant  |

#### Session Logout

| Method      | Route                | Authorized                    |
|:-----------:|----------------------|-------------------------------|
| POST or PUT | /session/logout      | Admin, Instructor, Assistant  |

#### 1st Half-month Time-sheet

| Method      | Route                                   | Authorized |
|:-----------:|-----------------------------------------|------------|
| GET         | /time-sheet/first-half-month            | Assistant  |
| GET         | /time-sheet/{*netid*}/first-half-month  | Instructor |

#### 2nd Half-month Time-Sheet

| Method      | Route                                    | Authorized |
|:-----------:|------------------------------------------|------------|
| GET         | /time-sheet/second-half-month            | Assistant  |
| GET         | /time-sheet/{*netid*}/second-half-month  | Instructor |


Data Models
===========

