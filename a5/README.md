# Assignment 5
_Kristian Moltke Reitzel, krei@itu.dk_

Report and source code is available [here](https://github.com/kmoltke/AIS-Assignments/tree/main/a5)

## Problem 1
### Part 1
File A:
- <Alice, {r, w}>
- <Bob, {r}>

File B:
- <Alice, {r}>
- <Bob, {r, w}>

File C:
- <Alice, {x}>
- <Bob, {}>

### Part 2
Alice:
- <FileA, {r, w}>
- <FileB, {r}>
- <FileC, {x}>

Bob:
- <FileA, {r}>
- <FileB, {r, w}>
- <FileC, {}>

### Part 3
Revoke all `write` permissions on file:
- For Acls you would have to update the Acls directly on that given file and remove all the write permissions for each user. 
- For Cls you would have to go through all users and check their Cls if they have write access to the given file and then change it.

Revoke all `write` permissions for user:
- For Acls you would have to go through ALL the Acls' that are stored on ALL files and check if the given user is in that Acls and then remove the write permisson.
- For Cls you would simply go to that users Cls and remove the write permission on all the files.

### Part 4

![img.png](screenshots/img.png)

## Problem 2
### Part 1
Domain: `dev-c6f6btnyigx2q7v8.eu.auth0.com`
Client ID: `PPwV1wzDOd8QujEbWCaM3R5JrFI64pUf`

