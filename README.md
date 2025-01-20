# TaskManagerBFF

## Install PostgreSQL

To install PostgreSQL on macOS, use Homebrew:

```bash
brew install postgresql
```

Start the PostgreSQL service:

```bash
brew services start postgresql
```

## Set Up PostgreSQL User and Password

After installing PostgreSQL, the superuser `postgres` does not have a password by default. You need to set it.

### Switch to the `postgres` User

Open a terminal and enter the PostgreSQL shell as the `postgres` user:

```bash
psql postgres
```

### Set a Password for `postgres`

Once inside the PostgreSQL shell, run the following command to set a password for the `postgres` superuser:

```sql
\password
postgres
```

## Create a Database Named `taskdb`

Now, create a new database called `taskdb`:

```sql
CREATE
DATABASE taskdb;
```

## Create Tables in the Database

After creating the database, you can create the necessary tables. First, switch to the `taskdb` database:

```sql
\c
taskdb
```

### Create the `roles` Table

```sql
CREATE TABLE roles
(
    role_id   SERIAL PRIMARY KEY,
    role_name TEXT NOT NULL
);
```

### Create the `statuses` Table

```sql
CREATE TABLE statuses
(
    status_id SERIAL PRIMARY KEY,
    status    TEXT NOT NULL
);
```

### Create the `users` Table

```sql
CREATE TABLE users
(
    user_id  SERIAL PRIMARY KEY,
    username TEXT    NOT NULL,
    password TEXT    NOT NULL,
    role_id  INTEGER NOT NULL REFERENCES roles (role_id) ON DELETE CASCADE
);
```

### Create the `tags` Table

```sql
CREATE TABLE tags
(
    tag_id SERIAL PRIMARY KEY,
    name   TEXT NOT NULL
);
```

### Create the `tasks` Table

```sql
CREATE TABLE tasks
(
    task_id   SERIAL PRIMARY KEY,
    task      TEXT    NOT NULL,
    status_id INTEGER NOT NULL REFERENCES statuses (status_id) ON DELETE CASCADE,
    user_id   INTEGER NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    tags      TEXT[] NOT NULL
);
```

## Insert Test Data

### Insert Data into `statuses`

```sql
INSERT INTO statuses (status)
VALUES ('Defined'),
       ('In-Progress'),
       ('Completed');
```

### Insert Data into `roles`

```sql
INSERT INTO roles (role_name)
VALUES ('Admin'),
       ('User');
```

### Insert Data into `users`

```sql
INSERT INTO users (username, password, role_id)
VALUES ('adam', 'password1', 1),
       ('bob', 'password2', 2),
       ('cyaa', 'password3', 2);
```

### Insert Data into `tags`

```sql
INSERT INTO tags (name)
VALUES ('tag1'),
       ('tag2'),
       ('tag3');
```

### Insert Data into `tasks`

```sql
INSERT INTO tasks (task, status_id, user_id, tags)
VALUES ('task1', 1, 2, ARRAY['tag1', 'tag2']),
       ('task2', 1, 2, ARRAY['tag2', 'tag3']),
       ('task3', 2, 2, ARRAY['tag1', 'tag3']);
```

### Exit the Shell

After setting the password, exit the PostgreSQL shell by typing:

```sql
\q
```

## Configuration Setup

### Update `application.conf` file from the below path

##### src/main/resources/application.conf

Add the following configurations:

```properties
serverPort=9090
serverHost=localhost
dbURL=jdbc:postgresql://localhost:5432/taskdb
dbUsername=postgres
dbPassword=postgres
```

---

## Setup ServerApp

Steps to Add a Run Configuration:

1. Open Run/Debug Configurations:
   • From the top-right dropdown menu (where run configurations are listed), click “Edit Configurations…”.
2. Add a New Application Configuration:
   • Click the + button in the top-left corner.
   • Select “Application” from the list.
3. Configure the Application:
   • Name: Provide a name for the configuration (e.g., ServerApp).
   • Java Version: Select the appropriate JDK.
   • Module: Choose the module that contains your ServerApp class.
   • Main Class: Set it to ServerApp.
4. Apply and Save:
   • Click “Apply” and then “OK” to save the configuration.
5. Run the Configuration:
   • Select the newly created configuration from the dropdown and click the green “Run” button.

---

## API Documentation

### Authentication

#### Login

**POST** `http://localhost:9090/login`

**Request Body:**

```json
{
  "username": "",
  "password": ""
}
```

**Response:**

```json
{
  "accessToken": "String",
  "userId": "String"
}
```

#### Token Refresh

**POST** `http://localhost:9090/refresh`

**Request Body:**

```json
{
  "token": ""
}
```

**Response:**

```json
{
  "accessToken": "String",
  "userId": "String"
}
```

---

### User Management

#### Get All Users

**GET** `http://localhost:9090/users`

#### Get User by ID

**GET** `http://localhost:9090/users/{id}`

#### Create User

**POST** `http://localhost:9090/users`

**Request Body:**

```json
{
  "username": "",
  "password": ""
}
```

#### Update User by ID

**PUT** `http://localhost:9090/users/{id}`

**Request Body:**

```json
{
  "username": "",
  "password": ""
}
```

#### Delete User by ID

**DELETE** `http://localhost:9090/users/{id}`

---

### Task Management

> **Note:** Include the Bearer Token in the header for all task-related routes.

#### Get All Tasks

**GET** `http://localhost:9090/tasks`

#### Get Task by ID

**GET** `http://localhost:9090/tasks/{id}`

#### Create Task

**POST** `http://localhost:9090/tasks`

**Request Body:**

```json
{
  "task": "",
  "status_id": "",
  "tags": [
    "tag1",
    "tag2",
    "tag3"
  ],
  "user_id": ""
}
```

#### Update Task by ID

**PUT** `http://localhost:9090/tasks/{id}`

**Request Body:**

```json
{
  "task": "",
  "status_id": "",
  "tags": [
    "tag1",
    "tag2",
    "tag3"
  ],
  "user_id": ""
}
```

#### Delete Task by ID

**DELETE** `http://localhost:9090/tasks/{id}`

---

### Tag Management

> **Note:** Include the Bearer Token in the header for all tag-related routes.

#### Get All Tags

**GET** `http://localhost:9090/tags`

#### Get Tag by ID

**GET** `http://localhost:9090/tags/{id}`

#### Create Tag

**POST** `http://localhost:9090/tags`

**Request Body:**

```json
{
  "name": ""
}
```

#### Update Tag by ID

**PUT** `http://localhost:9090/tags/{id}`

**Request Body:**

```json
{
  "name": ""
}
```

#### Delete Tag by ID

**DELETE** `http://localhost:9090/tags/{id}`