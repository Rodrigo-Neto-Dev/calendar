# calendar

A small Spring Boot meeting scheduler. Register an account, propose meetings,
accept invites, subscribe to your personal iCal feed, and discover real events
from Ticketmaster, SeatGeek, and Lisbon's Agenda Cultural — copy any of them
straight onto your calendar.

## Requirements

- **Java 17+**
- **Maven 3.9+**

The app uses an embedded H2 database (file-backed under `./data/`), so there's
nothing else to install.

## Run

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080>. Register an account at `/register`, log in,
and you're done.

URL map:

| Path | What |
| --- | --- |
| `/register`, `/login` | self-explanatory |
| `/calendar` | your meetings + pending invites |
| `/meetings/new` | propose a meeting |
| `/discover` | search Ticketmaster / SeatGeek / Agenda Cultural |
| `/ical/{token}.ics` | personal iCal feed (subscribable via `webcal://`) |

The iCal URL is shown at the bottom of `/calendar`. Paste it into Apple
Calendar, Google Calendar, or Outlook as a subscribed calendar to get every
meeting (including discovered events) synced read-only.

## Configuring event discovery

Discovery providers degrade gracefully — without keys, `/discover` still works
and uses Agenda Cultural de Lisboa (which needs no credentials). The other two
need API keys, both **free**.

### Ticketmaster (Discovery API)

1. Sign up at <https://developer.ticketmaster.com/>.
2. Once logged in, go to **My Account → My Apps**.
3. Copy the **Consumer Key** (the Consumer Secret isn't needed for the
   Discovery API).

### SeatGeek (Platform API)

1. Sign up at <https://platform.seatgeek.com/>.
2. Create an app — name and description are free-form, you can use
   `http://localhost:8080` as the App URL.
3. Copy the **Client ID** from your app's page.

### Wire them up

Pass the keys as environment variables when starting the app — that keeps
secrets out of `application.properties` and out of git:

```bash
TICKETMASTER_API_KEY=your_consumer_key \
SEATGEEK_CLIENT_ID=your_client_id \
mvn spring-boot:run
```

The Ticketmaster country filter defaults to **PT**; set
`TICKETMASTER_COUNTRY=` (blank) to query globally, or `TICKETMASTER_COUNTRY=US`
to target a different country.

## Project layout

```
src/main/java/com/example/meetings/
  config/         Spring Security
  controller/     HTTP entry points
  discover/       Event providers + DiscoveryService
  model/          JPA entities (User, Meeting, MeetingParticipant)
  repository/     Spring Data JPA
  service/        Business logic + iCal rendering
src/main/resources/
  application.properties
  static/css/     Stylesheet
  templates/      Thymeleaf views
```

## How meetings work

- Proposing a meeting blocks the slot on your calendar **immediately** (you
  auto-accept your own invite) and creates `PENDING` invites for everyone you
  invited. The meeting is marked **tentative**.
- When every invitee has accepted, the meeting flips to **confirmed**.
- Declined invites drop off the declinee's calendar but stay on the
  organizer's.
- Events copied from `/discover` go on as confirmed single-attendee meetings
  (you're inviting yourself), so they appear in your iCal feed straight away.

## Testing

The project includes a comprehensive test suite categorized using JUnit 5 tags:

| Tag | Description |
| --- | --- |
| `unit` | Fast, isolated tests for business logic. |
| `smoke` | Critical-path tests (Auth, meeting creation, context load). |
| `integration` | Spring context tests, repository interactions, and controllers. |
| `provider` | External event provider integration tests (Mocked). |
| `e2e` | Browser automation tests using Selenium (HtmlUnit). |

### Running Tests

Run all tests:
```bash
mvn test
```

Run a specific category:
```bash
mvn test -Dgroups="smoke"
```

Run multiple categories:
```bash
mvn test -Dgroups="unit | smoke"
```

## CI/CD

GitHub Actions are configured to run different test subsets depending on the trigger:

- **Feature Branch Pushes:** Runs `unit` and `smoke` tests for fast feedback.
- **Pull Requests:** Runs `unit`, `smoke`, and `integration` tests.
- **Pushes to main/master:** Runs the full test suite (`unit`, `smoke`, `integration`, `provider`, `e2e`).
