# 🧪 How to Run Tests

## Prerequisites

Make the test script executable:

```bash
chmod +x run-tests.sh
```

---

## Run Tests

### Run All Tests

```bash
./run-tests.sh
```

### Run a Specific Test Class

```bash
./mvnw test -Dtest=CourseServiceTest
```

### Generate Code Coverage Report

```bash
./mvnw clean test jacoco:report
```

Coverage report:

```text
target/site/jacoco/index.html
```

---

## Quick Test Commands

### Run All Tests

```bash
./mvnw test
```

### Run a Specific Test

```bash
./mvnw test -Dtest=CourseServiceTest
```

### Run Tests with Debug Output

```bash
./mvnw test -X
```

---

## Automation Script

Create a file named `run-tests.sh`:

```bash
#!/bin/bash

echo "========================================="
echo "Running Education API Tests"
echo "========================================="

# Unit Tests
echo ""
echo "📦 Running Unit Tests..."
./mvnw test -Dtest=*ServiceTest

# Integration Tests
echo ""
echo "🔗 Running Integration Tests..."
./mvnw test -Dtest=*IntegrationTest

# Coverage Report
echo ""
echo "📊 Running All Tests with Coverage Report..."
./mvnw clean test jacoco:report

echo ""
echo "✅ Tests completed!"
echo "📈 Coverage report: target/site/jacoco/index.html"
```

### Execute the Script

```bash
./run-tests.sh
```