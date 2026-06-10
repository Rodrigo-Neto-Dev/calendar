package com.example.meetings.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullFlowE2ETest {

    @LocalServerPort
    private int port;

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = new HtmlUnitDriver(true);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void registerLoginAndProposeMeeting() {
        String baseUrl = "http://localhost:" + port;
        String user = "e2e_" + System.currentTimeMillis();

        // Register
        driver.get(baseUrl + "/register");
        driver.findElement(By.name("username")).sendKeys(user);
        driver.findElement(By.name("email")).sendKeys(user + "@example.com");
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("form[action='/register'] button[type='submit']")).click();

        assertTrue(driver.getCurrentUrl().contains("/login"), "Should redirect to login after registration, but was: " + driver.getCurrentUrl());

        // Login
        driver.findElement(By.name("username")).sendKeys(user);
        driver.findElement(By.name("password")).sendKeys("password");
        driver.findElement(By.cssSelector("form[action='/login'] button[type='submit']")).click();

        assertTrue(driver.getCurrentUrl().contains("/calendar"), "Should redirect to calendar after login, but was: " + driver.getCurrentUrl() + "\nPage source: " + driver.getPageSource());

        // Propose Meeting
        driver.get(baseUrl + "/meetings/new");
        driver.findElement(By.name("title")).sendKeys("E2E Meeting");
        
        var js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("document.getElementById('start').value = '2099-01-01T10:00'");
        js.executeScript("document.getElementById('end').value = '2099-01-01T11:00'");

        driver.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        assertTrue(driver.getCurrentUrl().contains("/calendar"), "Should redirect to calendar after proposing meeting, but was: " + driver.getCurrentUrl());
        assertTrue(driver.getPageSource().contains("E2E Meeting"));
    }
}
