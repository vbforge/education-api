# Make script executable
chmod +x run-tests.sh

# Run all tests
./run-tests.sh

# Or run specific test
./mvnw test -Dtest=CourseServiceTest

# Run with coverage
./mvnw clean test jacoco:report

---

# quick test command

# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=CourseServiceTest

# Run with debug
./mvnw test -X

---

#!/bin/bash

echo "========================================="
echo "Running Education API Tests"
echo "========================================="

# Run unit tests
echo ""
echo "📦 Running Unit Tests..."
./mvnw test -Dtest=*ServiceTest

# Run integration tests
echo ""
echo "🔗 Running Integration Tests..."
./mvnw test -Dtest=*IntegrationTest

# Run all tests with coverage
echo ""
echo "📊 Running All Tests with Coverage Report..."
./mvnw clean test jacoco:report

echo ""
echo "✅ Tests completed!"
echo "📈 Coverage report: target/site/jacoco/index.html"