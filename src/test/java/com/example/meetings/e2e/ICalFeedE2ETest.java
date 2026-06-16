package com.example.meetings.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;

/**
 * E2E test for the iCal feed feature.
 * Verifies that after creating a meeting, the iCal feed contains the event,
 * and that invalid tokens return 404.
 *
 * NOTE: The invalid token test currently documents that the application returns
 * 302 (redirect) instead of 404 for invalid iCal tokens. This is a potential bug
 * where the ResponseStatusException(NOT_FOUND) may be handled by Spring Boot's
 * default error handling differently than expected in the full server context.
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ICalFeedE2ETest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        driver = new HtmlUnitDriver(true);
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void icalFeed_AfterCreatingMeeting_ContainsEvent() {
        String baseUrl = "http://localhost:" + port;
        String user = "ical_e2e_" + System.currentTimeMillis();

        // Register
        driver.get(baseUrl + "/register");
        driver.findElement(By.name("username")).sendKeys(user);
        driver.findElement(By.name("email")).sendKeys(user + "@example.com");
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Login
        driver.findElement(By.name("username")).sendKeys(user);
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Propose Meeting
        driver.get(baseUrl + "/meetings/new");
        driver.findElement(By.name("title")).sendKeys("iCal Test Meeting");
        var js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("document.getElementById('start').value = '2099-01-01T10:00'");
        js.executeScript("document.getElementById('end').value = '2099-01-01T11:00'");
        driver.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        // Extract iCal token from calendar page
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iCal"), "Calendar page should show iCal feed link");

        // The token is in the page as part of the URL — extract it via a simple regex-like search
        String token = extractToken(pageSource);
        assertTrue(token != null && !token.isBlank(), "iCal token should be present on calendar page");

        // Fetch the iCal feed via HTTP
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/ical/" + token + ".ics"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "iCal feed should return 200");
            String body = response.body();
            assertTrue(body.contains("BEGIN:VCALENDAR"), "Response should be valid iCal");
            assertTrue(body.contains("iCal Test Meeting"), "iCal should contain the meeting title");
            assertTrue(body.contains("STATUS:CONFIRMED"), "Single-attendee meeting should be confirmed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void icalFeed_InvalidToken_ReturnsNotFound() {
        String baseUrl = "http://localhost:" + port;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/ical/invalid-token.ics"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(404, response.statusCode(), "Invalid token should return 404");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractToken(String pageSource) {
        // Look for the token in the page source: /ical/TOKEN.ics
        int start = pageSource.indexOf("/ical/");
        if (start == -1) return null;
        start += "/ical/".length();
        int end = pageSource.indexOf(".ics", start);
        if (end == -1) return null;
        return pageSource.substring(start, end);
    }
}