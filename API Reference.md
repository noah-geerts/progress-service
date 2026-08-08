# Progress API Documentation

This API manages workout sessions, exercises, performed exercises, and sets for advanced lifters.

## Base URL

```
http://localhost:3000
```

## Authentication

All endpoints require JWT authentication via Auth0, except for public endpoints under `/public/*`.

---

## Session Endpoints

### Get Session by Date

```http
GET /sessions/{date}
```

**Parameters:**

- `date` (path parameter): Date in ISO format (YYYY-MM-DD)

**Response:**
All Performed Exercises will be sorted by order increasing, as well as all sets within them.

```json
{
  "ssid": 123,
  "date": "2024-09-25",
  "name": "Push Day",
  "uid": "auth0|user123",
  "performedExercises": [
    {
      "peid": 456,
      "position": 1,
      "exercise": {
        "eid": 789,
        "name": "Bench Press"
      },
      "sets": [
        {
          "stid": 101,
          "position": 1,
          "reps": 8,
          "weight": 225.5
        }
      ]
    }
  ]
}
```

**Error Responses:**

- 404 Not Found if the Session does not exist

### Get Monthly Sessions

```http
GET /sessions/monthly/{date}
```

Retrieves all sessions for the month containing the specified date. Only returns sessions that exist; days without sessions are omitted from the response.

**Parameters:**

- `date` (path parameter): Any date in ISO format (YYYY-MM-DD) within the desired month

**Response:**

Returns an array of sessions for all days in the month that have sessions. Each session includes performed exercises sorted by position (ascending), with sets also sorted by position (ascending).

```json
[
  {
    "ssid": 123,
    "date": "2024-09-05",
    "name": "Push Day",
    "uid": "auth0|user123",
    "performedExercises": [
      {
        "peid": 456,
        "position": 1,
        "exercise": {
          "eid": 789,
          "name": "Bench Press"
        },
        "sets": [
          {
            "stid": 101,
            "position": 1,
            "reps": 8,
            "weight": 225.5
          }
        ]
      }
    ]
  },
  {
    "ssid": 124,
    "date": "2024-09-07",
    "name": "Pull Day",
    "uid": "auth0|user123",
    "performedExercises": [
      {
        "peid": 457,
        "position": 1,
        "exercise": {
          "eid": 790,
          "name": "Deadlift"
        },
        "sets": [
          {
            "stid": 102,
            "position": 1,
            "reps": 5,
            "weight": 315.0
          }
        ]
      }
    ]
  }
]
```

**Success Response:**

- 200 OK with array of SessionResponseDto objects (may be empty if no sessions exist in the month)

**Example Usage:**

```http
GET /sessions/monthly/2024-09-15
```

This will return all sessions from September 1-30, 2024.

**Notes:**

1. The date parameter can be any day within the desired month
2. Returns an empty array `[]` if no sessions exist in the specified month
3. Sessions are returned in chronological order by date
4. Only sessions belonging to the authenticated user are returned
5. Each session's performed exercises and sets are sorted by their position field (ascending)

### Create Session

```http
POST /sessions{date}
```

**Parameters:**

- `date` (path parameter): Date in ISO format (YYYY-MM-DD)

**Request Body:**

```json
{
  "name": "Push Day"
}
```

**Response:**

```json
{
  "ssid": 123,
  "date": "2024-09-25",
  "name": "Push Day",
  "uid": "auth0|user123",
  "performedExercises": []
}
```

**Error Responses:**

- 409 Conflict if there is already a session for the given date

### Update Session

```http
PATCH /sessions/{date}
```

**Parameters:**

- `date` (path parameter): Date in ISO format (YYYY-MM-DD)

**Request Body:**

```json
{
  "name": "Updated Push Day"
}
```

**Response:**

```json
{
  "ssid": 123,
  "date": "2024-09-25",
  "name": "Updated Push Day",
  "uid": "auth0|user123",
  "performedExercises": []
}
```

**Error Responses:**

- 404 Not Found if there is no session yet on the given day

### Delete Session

```http
DELETE /sessions/{date}
```

**Parameters:**

- `date` (path parameter): Date in ISO format (YYYY-MM-DD)

**Response:**

```http
204 No Content
```

**Error Responses:**

- 404 Not Found if there is no session yet on the given day

---

## Exercise Endpoints

### Get All Exercises

```http
GET /exercises
```

**Response:**

Exercises will be sorted alphabetically by name

```json
[
  {
    "eid": 789,
    "name": "Bench Press"
  },
  {
    "eid": 790,
    "name": "Squat"
  }
]
```

### Create Exercise

```http
POST /exercises
```

**Request Body:**

```json
{
  "name": "Deadlift"
}
```

**Response:**

```json
{
  "eid": 791,
  "name": "Deadlift"
}
```

**Error Responses:**

- 409 Conflict if there is already an exercise with the given name

### Update Exercise

```http
PATCH /exercises/{eid}
```

**Parameters:**

- `eid` (path parameter): Exercise ID

**Request Body:**

```json
{
  "name": "Updated Exercise"
}
```

**Response:**

```json
{
  "eid": 791,
  "name": "Updated Exercise"
}
```

**Error Responses:**

- 404 Not Found if there is no exercise with the given eid
- 409 Conflict if there is already an exercise with the given name

### Delete Exercise

```http
DELETE /exercises/{eid}
```

**Parameters:**

- `eid` (path parameter): Exercise ID

**Response:**

```http
204 No Content
```

**Error Responses:**

- 404 Not Found if there is no exercise with the given eid
- 422 Unprocessable Entity if the exercise cannot be deleted because it is used in Performed Exercises

---

## Performed Exercise Endpoints

### Create Performed Exercise

```http
POST /performed-exercises
```

**Request Body:**

```json
{
  "eid": 789,
  "ssid": 123,
  "position": 1
}
```

**Response:**

```json
{
  "peid": 456,
  "position": 1,
  "exercise": {
    "eid": 789,
    "name": "Bench Press"
  },
  "sets": []
}
```

**Error Responses:**

- 409 Conflict if there is already a Performed Exercise with the given ssid and order
- 422 Unprocessable Entity if the ssid or eid do not correspond to a valid session or exercise

### Update Performed Exercise

```http
PATCH /performed-exercises/{peid}
```

**Parameters:**

- `peid` (path parameter): Performed Exercise ID

**Request Body:**

```json
{
  "eid": 790
}
```

**Response:**

```json
{
  "peid": 456,
  "position": 1,
  "exercise": {
    "eid": 790,
    "name": "Squat"
  },
  "sets": []
}
```

**Error Responses:**

- 422 Unprocessable Entity if the eid does not correspond to a valid exercise
- 404 Not Found if the Performed Exercise does not exist

### Delete Performed Exercise

```http
DELETE /performed-exercises/{peid}
```

**Parameters:**

- `peid` (path parameter): Performed Exercise ID

**Response:**

```http
204 No Content
```

**Error Responses:**

- 404 Not Found if the Performed Exercise does not exist

---

## PerformedSet Endpoints

### Create PerformedSet

```http
POST /sets
```

**Request Body:**

```json
{
  "peid": 456,
  "position": 1,
  "reps": 8,
  "weight": 225.5
}
```

**Response:**

```json
{
  "stid": 101,
  "position": 1,
  "reps": 8,
  "weight": 225.5
}
```

**Error Responses:**

- 409 Conflict if a Set already exists with the given order and peid
- 422 Unprocessable Entity if the provided peid does not correspond to a valid Performed Exercise

### Update PerformedSet

```http
PATCH /sets/{stid}
```

**Parameters:**

- `stid` (path parameter): PerformedSet ID

**Request Body:**

```json
{
  "reps": 10,
  "weight": 230.0
}
```

**Response:**

```json
{
  "stid": 101,
  "position": 1,
  "reps": 10,
  "weight": 230.0
}
```

**Error Responses:**

- 404 Not Found if the set with the given stid does not exist

### Delete PerformedSet

```http
DELETE /sets/{stid}
```

**Parameters:**

- `stid` (path parameter): PerformedSet ID

**Response:**

```http
204 No Content
```

**Error Responses:**

- 404 Not Found if the set with the given stid does not exist

---

## Data Transfer Objects (DTOs)

### Session DTOs

#### SessionRequestDto

Used for creating and updating sessions.

```json
{
  "name": "string"
}
```

#### SessionResponseDto

```json
{
  "ssid": "number",
  "date": "string (ISO date)",
  "name": "string",
  "uid": "string",
  "performedExercises": "PerformedExerciseResponseDto[]"
}
```

### Exercise DTOs

#### CreateExerciseDto

```json
{
  "name": "string"
}
```

#### ExerciseResponseDto

```json
{
  "eid": "number",
  "name": "string"
}
```

### Performed Exercise DTOs

#### CreatePerformedExerciseDto

```json
{
  "eid": "number",
  "ssid": "number",
  "position": "number"
}
```

#### UpdatePerformedExerciseDto

```json
{
  "eid": "number"
}
```

#### PerformedExerciseResponseDto

```json
{
  "peid": "number",
  "position": "number",
  "exercise": "ExerciseResponseDto",
  "sets": "SetResponseDto[]"
}
```

### PerformedSet DTOs

#### CreateSetDto

```json
{
  "peid": "number",
  "position": "number",
  "reps": "number",
  "weight": "number"
}
```

#### UpdateSetDto

```json
{
  "reps": "number",
  "weight": "number"
}
```

#### SetResponseDto

```json
{
  "stid": "number",
  "position": "number",
  "reps": "number",
  "weight": "number"
}
```

## Notes

1. All endpoints require authentication except those under `/public/*`
2. Unauthorized requests will receive a 401 response
3. All requests with fields that do not match the backend Dto will receive 400 Bad Request when Jakarta validation catches the incorrect field(s)
4. Date parameters should be in ISO format (YYYY-MM-DD)
5. The `uid` field is automatically populated from the authenticated user's JWT token
6. When creating sessions, the `date` is automatically set to the current date
7. Deleting a session will cascade delete all associated performed exercises and sets
8. Deleting a performed exercise will cascade delete all associated sets
9. The `position` field determines the sequence of performed exercises within a session and sets within a performed exercise
