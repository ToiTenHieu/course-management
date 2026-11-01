const { defineConfig, devices } = require("@playwright/test");
// E2E creates accounts and courses on every run, so the managed server uses its own
// port and database instead of the demo at 8080 / course_management_demo.
const port = process.env.E2E_PORT || "8090";
const databaseUrl =
  process.env.E2E_DB_URL ||
  "jdbc:postgresql://localhost:5432/course_management_e2e";
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
    baseURL: process.env.E2E_BASE_URL || `http://127.0.0.1:${port}`,
    timezoneId: "UTC",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command:
          "java -jar target/course_management-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo" +
          ` --server.port=${port} --spring.datasource.url=${databaseUrl}` +
          " --spring.config.additional-location=optional:file:target/local-demo/application-local.properties",
        url: `http://127.0.0.1:${port}/api/auth/ready`,
        reuseExistingServer: false,
        timeout: 120000,
      },
});
