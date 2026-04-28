# OAuth2 Integration Guide (Google & GitHub)

## Overview

This project now supports OAuth2 login with Google and GitHub. This guide explains:
1. How to set up OAuth2 credentials in Google Console and GitHub
2. How to configure the backend and frontend
3. How to test the OAuth2 flow

---

## Backend Configuration

### 1. Add OAuth2 Dependencies (✓ Already Done)

The following dependencies have been added to `pom.xml`:
- `spring-boot-starter-security`
- `spring-security-oauth2-client`
- `spring-security-oauth2-jose`

### 2. Environment Variables Configuration

Update your `.env` file or `application.properties` with OAuth2 credentials:

```properties
# Google OAuth2
OAUTH2_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID_HERE
OAUTH2_GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET_HERE

# GitHub OAuth2
OAUTH2_GITHUB_CLIENT_ID=YOUR_GITHUB_CLIENT_ID_HERE
OAUTH2_GITHUB_CLIENT_SECRET=YOUR_GITHUB_CLIENT_SECRET_HERE
```

### 3. Features Implemented

- **OAuth2SuccessHandler**: Manages successful OAuth2 authentication
  - Checks if user exists in database
  - Creates new user if doesn't exist
  - Updates existing user with latest photo URL
  - Generates JWT token
  - Redirects to frontend with token as query parameter

- **OAuth2FailureHandler**: Handles authentication failures
  - Redirects to login page with error message

- **SecurityConfig**: Spring Security configuration
  - Configures OAuth2 login
  - Enables CORS for frontend communication
  - Permits public endpoints

---

## Frontend Configuration

### 1. Environment Setup

The frontend `environment.development.ts` should include the API URL:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081'
};
```

### 2. Components Implemented

- **OAuth2ButtonsComponent**: Reusable component with Google and GitHub buttons
  - Styled with Tailwind CSS
  - Can be used on any authentication page

- **OAuth2CallbackComponent**: Handles OAuth2 callback
  - Extracts token from query parameters
  - Stores token and user info in localStorage
  - Redirects to dashboard

- **AuthService**: Updated with OAuth2 methods
  - `loginWithOAuth2(provider)` - Initiates OAuth2 login
  - `getOAuth2UserInfo()` - Retrieves stored user info
  - `logout()` - Clears all auth data

### 3. Pages Updated

- **Sign In (Member Login)**: Added OAuth2 buttons below login form
- **Sign Up (Club Application)**: Added OAuth2 buttons below application form

---

## Google OAuth2 Setup

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (e.g., "Cluverse OAuth2")
3. Enable the Google+ API

### Step 2: Create OAuth2 Credentials

1. Go to **Credentials** > **Create Credentials** > **OAuth 2.0 Client ID**
2. Choose **Web application**
3. Add Authorized redirect URIs:
   ```
   http://localhost:8081/login/oauth2/code/google
   https://yourdomain.com/login/oauth2/code/google
   ```
4. Copy **Client ID** and **Client Secret**

### Step 3: Configure Backend

Add to `.env` or `application.properties`:

```properties
OAUTH2_GOOGLE_CLIENT_ID=1234567890-abcdefg.apps.googleusercontent.com
OAUTH2_GOOGLE_CLIENT_SECRET=GOCSPX_xxxxxxxxxx
```

---

## GitHub OAuth2 Setup

### Step 1: Create GitHub OAuth App

1. Go to GitHub Settings > **Developer settings** > **OAuth Apps** > **New OAuth App**
2. Fill in:
   - **Application name**: Cluverse
   - **Homepage URL**: http://localhost:4200
   - **Authorization callback URL**: http://localhost:8081/login/oauth2/code/github
3. Copy **Client ID** and **Client Secret**

### Step 2: Configure Backend

Add to `.env` or `application.properties`:

```properties
OAUTH2_GITHUB_CLIENT_ID=Iv1.abcdefghijk
OAUTH2_GITHUB_CLIENT_SECRET=1234567890abcdefghijk
```

---

## Testing OAuth2 Flow

### Local Testing

1. **Start Backend**: 
   ```bash
   mvn spring-boot:run
   ```

2. **Start Frontend**:
   ```bash
   npm start
   ```

3. **Navigate to Sign In**: http://localhost:4200/auth/login

4. **Click "Sign in with Google" or "Sign in with GitHub"**

5. **Expected Flow**:
   - You'll be redirected to Google/GitHub login
   - After authentication, you'll be redirected to `/auth/oauth2-callback`
   - Token will be extracted and stored in localStorage
   - You'll be redirected to `/dashboard`

### Troubleshooting

**Issue**: CORS errors
- **Solution**: Ensure `SecurityConfig` has correct CORS configuration and frontend URL is whitelisted

**Issue**: "Invalid redirect URI"
- **Solution**: Check that the redirect URIs in Google Console / GitHub Settings match exactly

**Issue**: "User not found" after OAuth2 login
- **Solution**: Check that the User entity has `findByEmail()` method in repository

**Issue**: No token received at callback
- **Solution**: Check browser console for errors; ensure OAuth2SuccessHandler is properly configured

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Angular)                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ OAuth2ButtonsComponent                                   ││
│  │  ├─ Google Button → /oauth2/authorization/google         ││
│  │  └─ GitHub Button → /oauth2/authorization/github         ││
│  └─────────────────────────────────────────────────────────┘│
│                          ↓                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ OAuth2CallbackComponent                                  ││
│  │  ├─ Extracts token from query params                     ││
│  │  ├─ Stores in localStorage                               ││
│  │  └─ Redirects to /dashboard                              ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
           ↑                                        ↓
           │ OAuth2 Flow                            │
           │                                        │ Callback
           │                                        │ + Token
┌─────────────────────────────────────────────────────────────┐
│                   Backend (Spring Boot)                     │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ /oauth2/authorization/{google/github}                  ││
│  │  └─ Delegates to Spring OAuth2                          ││
│  └─────────────────────────────────────────────────────────┘│
│                          ↓                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ OAuth2SuccessHandler                                     ││
│  │  ├─ Check user exists                                    ││
│  │  ├─ Create/Update user                                   ││
│  │  ├─ Generate JWT                                         ││
│  │  └─ Redirect to /auth/oauth2-callback?token=xxx          ││
│  └─────────────────────────────────────────────────────────┘│
│                          ↓                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ User Repository                                          ││
│  │  ├─ Find by email                                        ││
│  │  └─ Save new/updated user                                ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
           ↑
           │ OAuth2 Providers
           │ (Google / GitHub)
           │
┌─────────────────────────────────────────────────────────────┐
│              OAuth2 Providers                               │
│  ├─ Google (accounts.google.com)                            │
│  └─ GitHub (github.com)                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## Security Considerations

1. **JWT Token**: Generated by backend and stored in localStorage
   - Use `Authorization: Bearer <token>` header for protected API calls

2. **Redirect URI**: Must be HTTPS in production

3. **Client Secret**: Never expose in frontend code; keep in backend environment variables

4. **CORS**: Configured to allow frontend domain only

---

## Production Deployment

### Backend

1. Update `application-prod.properties` with production URLs:
```properties
app.base-url=https://yourdomain.com
OAUTH2_GOOGLE_CLIENT_ID=prod-google-id
OAUTH2_GOOGLE_CLIENT_SECRET=prod-google-secret
OAUTH2_GITHUB_CLIENT_ID=prod-github-id
OAUTH2_GITHUB_CLIENT_SECRET=prod-github-secret
```

2. Update Google Console and GitHub Settings with production redirect URIs:
```
https://yourdomain.com/login/oauth2/code/google
https://yourdomain.com/login/oauth2/code/github
```

### Frontend

1. Update `environment.prod.ts`:
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.yourdomain.com'
};
```

---

## File Changes Summary

### Backend Files Created/Modified

- `pom.xml` - Added OAuth2 dependencies
- `src/main/resources/application.properties` - Added OAuth2 configuration
- `src/main/java/com/hexaweb/backendcluverse/config/SecurityConfig.java` - NEW
- `src/main/java/com/hexaweb/backendcluverse/config/oauth2/OAuth2SuccessHandler.java` - NEW
- `src/main/java/com/hexaweb/backendcluverse/config/oauth2/OAuth2FailureHandler.java` - NEW
- `src/main/java/com/hexaweb/backendcluverse/dto/OAuth2UserRequest.java` - NEW
- `src/main/java/com/hexaweb/backendcluverse/dto/OAuth2CallbackResponse.java` - NEW

### Frontend Files Created/Modified

- `src/app/features/auth/components/oauth2-buttons/oauth2-buttons.component.ts` - NEW
- `src/app/features/auth/components/oauth2-buttons/oauth2-buttons.component.html` - NEW
- `src/app/features/auth/components/oauth2-buttons/oauth2-buttons.component.scss` - NEW
- `src/app/features/auth/components/oauth2-callback/oauth2-callback.component.ts` - NEW
- `src/app/features/auth/auth.module.ts` - UPDATED
- `src/app/features/auth/auth-routing.module.ts` - UPDATED
- `src/app/core/services/auth.service.ts` - UPDATED
- `src/app/features/auth/components/member-login/member-login.component.html` - UPDATED
- `src/app/features/auth/components/login/login.component.html` - UPDATED

---

## Next Steps

1. ✅ Obtain Google OAuth2 credentials
2. ✅ Obtain GitHub OAuth2 credentials
3. ✅ Add credentials to `.env` file
4. ✅ Test OAuth2 flow locally
5. ✅ Deploy to production
6. ✅ Update production OAuth2 credentials

For support or issues, check the troubleshooting section above.
