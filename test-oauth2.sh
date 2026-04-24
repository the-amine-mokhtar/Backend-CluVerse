#!/bin/bash
# OAuth2 Testing Commands

echo "🚀 OAuth2 Testing Script"
echo "======================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Backend tests
echo -e "\n${YELLOW}[1] Testing Backend OAuth2 Endpoints${NC}"

echo "Checking if backend is running on port 8081..."
if curl -s http://localhost:8081/swagger-ui.html > /dev/null; then
    echo -e "${GREEN}✓ Backend is running${NC}"
else
    echo -e "${RED}✗ Backend is NOT running${NC}"
    echo "Start backend with: cd Backend-Cluverse && mvn spring-boot:run"
    exit 1
fi

echo -e "\n${YELLOW}[2] Testing OAuth2 Endpoints${NC}"

echo "Testing Google OAuth2 endpoint..."
curl -i -s http://localhost:8081/oauth2/authorization/google 2>/dev/null | head -n 10

echo -e "\n\nTesting GitHub OAuth2 endpoint..."
curl -i -s http://localhost:8081/oauth2/authorization/github 2>/dev/null | head -n 10

# Frontend tests
echo -e "\n${YELLOW}[3] Testing Frontend${NC}"

echo "Checking if frontend is running on port 4200..."
if curl -s http://localhost:4200 > /dev/null; then
    echo -e "${GREEN}✓ Frontend is running${NC}"
else
    echo -e "${RED}✗ Frontend is NOT running${NC}"
    echo "Start frontend with: cd Frontend-Cluverse && npm start"
    exit 1
fi

echo -e "\n${YELLOW}[4] Opening Login Page${NC}"
echo "Opening http://localhost:4200/auth/login in browser..."
echo "Look for Google and GitHub buttons"

# Detect OS and open browser
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    xdg-open http://localhost:4200/auth/login
elif [[ "$OSTYPE" == "darwin"* ]]; then
    open http://localhost:4200/auth/login
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    start http://localhost:4200/auth/login
fi

echo -e "\n${YELLOW}[5] Manual Testing Instructions${NC}"
echo "1. Click 'Sign in with Google' or 'Sign in with GitHub'"
echo "2. Enter your credentials"
echo "3. Authorize the application"
echo "4. You should be redirected to /auth/oauth2-callback"
echo "5. Then redirected to /dashboard"
echo -e "\n6. Check DevTools (F12) > Application > localStorage"
echo "   Should see: 'token' key with JWT value"

echo -e "\n${YELLOW}[6] Database Verification${NC}"
echo "Check MySQL for new user:"
echo "SELECT * FROM user WHERE email = 'your-authenticated-email';"

echo -e "\n${GREEN}✓ Testing complete!${NC}"
