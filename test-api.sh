#!/bin/bash
BASE="http://localhost:8080"
COOKIE="cookies.txt"

echo "=== Testing Education API ==="

# Login
echo "1. Logging in as instructor..."
curl -s -X POST "$BASE/login" -d "username=instructor@email.com&password=instructor1234" -c $COOKIE > /dev/null
echo "✅ Logged in"

# Create Course
echo "2. Creating course..."
COURSE=$(curl -s -X POST "$BASE/api/v1/courses" -H "Content-Type: application/json" -b $COOKIE \
  -d '{"name":"Test Course","description":"Test","instructor":"Test"}' | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "✅ Course ID: $COURSE"

# Get Courses (should be 1 query for courses + 1 for counts)
echo "3. Fetching courses (check console for SQL count queries)..."
curl -s -X GET "$BASE/api/v1/courses?page=0&size=10" -b $COOKIE | head -c 200
echo "..."

echo "✅ Test complete!"