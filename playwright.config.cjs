const { defineConfig, devices } = require("@playwright/test");
module.exports = defineConfig({
  testDir: "./tests/e2e",
  timeout: 45000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [
    ["list"],
    ["html", { open: "never", outputFolder: "output/e2e-report" }],
  ],
  outputDir: "output/e2e-results",
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://127.0.0.1:8080",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command:
          "java -jar target/course_management-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo",
        url: "http://127.0.0.1:8080/api/auth/config",
        reuseExistingServer: false,
        timeout: 120000,
      },
});
