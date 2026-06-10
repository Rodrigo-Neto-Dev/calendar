package com.example.meetings.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpandedE2ETest {

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
    void discoveryAndLogoutJourney() {
        String baseUrl = "http://localhost:" + port;
        String user = "e2e_expanded_" + System.currentTimeMillis();

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

        // Discovery (assuming at least one provider is configured or it shows "none configured")
        driver.get(baseUrl + "/discover");
        assertTrue(driver.getPageSource().contains("Discover"), "Should be on discovery page");
        
        // Search
        driver.findElement(By.name("q")).sendKeys("music");
        driver.findElement(By.cssSelector("form[action='/discover'] button[type='submit']")).click();
        
        // Even if no results, we verify we are still on the page
        assertTrue(driver.getCurrentUrl().contains("/discover"), "Should remain on discovery page after search");

        // Logout
        driver.findElement(By.cssSelector("form[action='/logout'] button")).click();
        assertTrue(driver.getCurrentUrl().contains("/login?logout"), "Should be on login page after logout");

        // Try access protected page
        driver.get(baseUrl + "/calendar");
        assertTrue(driver.getCurrentUrl().contains("/login"), "Should be redirected to login when accessing protected page after logout");
    }

    @Test
    void createMeetingAndVerifyInCalendar() {
        String baseUrl = "http://localhost:" + port;
        String user = "e2e_meeting_" + System.currentTimeMillis();

        // Register & Login
        driver.get(baseUrl + "/register");
        driver.findElement(By.name("username")).sendKeys(user);
        driver.findElement(By.name("email")).sendKeys(user + "@example.com");
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        driver.findElement(By.name("username")).sendKeys(user);
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Propose Meeting
        driver.get(baseUrl + "/meetings/new");
        driver.findElement(By.name("title")).sendKeys("Specific E2E Meeting");
        
        var js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("document.getElementById('start').value = '2099-01-01T12:00'");
        js.executeScript("document.getElementById('end').value = '2099-01-01T13:00'");

        driver.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        // Verify in Calendar
        assertTrue(driver.getCurrentUrl().contains("/calendar"), "Should be on calendar page, but was: " + driver.getCurrentUrl());
        assertTrue(driver.getPageSource().contains("Specific E2E Meeting"), "Meeting title should be visible");
    }
}
