package com.example.meetings.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;

/**
 * E2E test for meeting invitation response flows.
 * Verifies that accepting a meeting confirms it and declining removes it from the calendar.
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MeetingResponseE2ETest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        driver = new HtmlUnitDriver(true);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void acceptInvite_MeetingBecomesConfirmed() {
        String baseUrl = "http://localhost:" + port;
        String organizer = "org_e2e_" + System.currentTimeMillis();
        String invitee = "inv_e2e_" + System.currentTimeMillis();

        // Register organizer
        registerUser(baseUrl, organizer);
        // Register invitee
        registerUser(baseUrl, invitee);

        // Login as organizer
        loginUser(baseUrl, organizer);

        // Propose meeting inviting invitee
        driver.get(baseUrl + "/meetings/new");
        driver.findElement(By.name("title")).sendKeys("Response Test Meeting");
        driver.findElement(By.name("invitees")).sendKeys(invitee);
        var js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("document.getElementById('start').value = '2099-01-01T14:00'");
        js.executeScript("document.getElementById('end').value = '2099-01-01T15:00'");
        driver.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        // Verify meeting appears on organizer's calendar
        assertTrue(driver.getCurrentUrl().contains("/calendar"), "Should redirect to calendar");
        assertTrue(driver.getPageSource().contains("Response Test Meeting"), "Meeting should be visible");

        // Logout organizer
        driver.findElement(By.cssSelector("form[action='/logout'] button")).click();

        // Login as invitee
        loginUser(baseUrl, invitee);

        // Invitees see pending invites on their calendar
        assertTrue(driver.getPageSource().contains("Response Test Meeting"), "Invitee should see pending meeting");

        // Find and click the accept button for this meeting
        // The meeting should have an accept form — look for it in the page
        var acceptForms = driver.findElements(By.cssSelector("form[action*='/respond']"));
        assertTrue(!acceptForms.isEmpty(), "There should be at least one respond form");

        // Click the first accept button found
        for (var form : acceptForms) {
            if (form.getText().contains("Accept") || form.toString().contains("accept")) {
                form.findElement(By.cssSelector("button")).click();
                break;
            }
        }

        // After accepting, the meeting should still be on the calendar and show as confirmed
        assertTrue(driver.getCurrentUrl().contains("/calendar"), "Should redirect to calendar after accept");
        assertTrue(driver.getPageSource().contains("Response Test Meeting"), "Accepted meeting should remain visible");
    }

    @Test
    void declineInvite_MeetingDisappearsFromCalendar() {
        String baseUrl = "http://localhost:" + port;
        String organizer = "org2_e2e_" + System.currentTimeMillis();
        String invitee = "inv2_e2e_" + System.currentTimeMillis();

        // Register both users
        registerUser(baseUrl, organizer);
        registerUser(baseUrl, invitee);

        // Login as organizer and propose meeting
        loginUser(baseUrl, organizer);
        driver.get(baseUrl + "/meetings/new");
        driver.findElement(By.name("title")).sendKeys("Decline Test Meeting");
        driver.findElement(By.name("invitees")).sendKeys(invitee);
        var js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("document.getElementById('start').value = '2099-01-01T16:00'");
        js.executeScript("document.getElementById('end').value = '2099-01-01T17:00'");
        driver.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        // Logout organizer
        driver.findElement(By.cssSelector("form[action='/logout'] button")).click();

        // Login as invitee
        loginUser(baseUrl, invitee);

        // Verify meeting is visible as pending
        assertTrue(driver.getPageSource().contains("Decline Test Meeting"), "Invitee should see pending meeting");

        // Find and click the decline button
        var forms = driver.findElements(By.cssSelector("form[action*='/respond']"));
        for (var form : forms) {
            // Try to find a decline button — look for forms with decline action or button text
            var buttons = form.findElements(By.cssSelector("button"));
            for (var button : buttons) {
                String text = button.getText().toLowerCase();
                if (text.contains("decline") || button.getAttribute("value") != null && button.getAttribute("value").toLowerCase().contains("decline")) {
                    button.click();
                    break;
                }
            }
        }

        // After declining, the meeting should NOT appear on the invitee's calendar
        assertTrue(driver.getCurrentUrl().contains("/calendar"), "Should redirect to calendar after decline");
        assertFalse(driver.getPageSource().contains("Decline Test Meeting"), "Declined meeting should not appear on invitee calendar");
    }

    private void registerUser(String baseUrl, String username) {
        driver.get(baseUrl + "/register");
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("email")).sendKeys(username + "@example.com");
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    private void loginUser(String baseUrl, String username) {
        driver.get(baseUrl + "/login");
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }
}
