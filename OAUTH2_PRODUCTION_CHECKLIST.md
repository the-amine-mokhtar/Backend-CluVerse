# 🚀 OAuth2 Production Deployment Checklist

## Pre-Deployment

### Backend Preparation
- [ ] Update `application-prod.properties` with production environment
- [ ] Ensure all OAuth2 dependencies are in `pom.xml`
- [ ] Test OAuth2 flow locally with final credentials
- [ ] Review `SecurityConfig.java` for production readiness
- [ ] Update CORS allowed origins for production domain
- [ ] Verify database migrations are up to date
- [ ] Test User creation/update logic

### Frontend Preparation
- [ ] Update `environment.prod.ts` with production API URL
- [ ] Test OAuth2 buttons are properly styled
- [ ] Verify callback routing to /dashboard
- [ ] Test token storage and retrieval
- [ ] Build Angular project: `npm run build`
- [ ] Check for console errors in prod build
- [ ] Verify OAuth2 components are properly imported

### Infrastructure
- [ ] SSL/HTTPS certificate ready
- [ ] Backend server URL confirmed (e.g., https://api.yourdomain.com)
- [ ] Frontend server URL confirmed (e.g., https://yourdomain.com)
- [ ] Database ready for production
- [ ] Environment variables configured on server

---

## Google OAuth2 Setup (Production)

### Step 1: Update Google Cloud Project
- [ ] Go to Google Cloud Console (same project or new)
- [ ] Verify Google+ API is enabled
- [ ] Go to Credentials → OAuth 2.0 Client ID
- [ ] Update Authorized redirect URIs:
  ```
  https://api.yourdomain.com/login/oauth2/code/google
  https://yourdomain.com/login/oauth2/code/google (optional, for direct access)
  ```
- [ ] Save changes
- [ ] Verify Client ID and Client Secret are correct
- [ ] Note: Can keep same credentials or create new ones
- [ ] Update consent screen if needed (OAuth consent screen)

### Step 2: Configure Backend
- [ ] Add to `.env` or environment variables:
  ```
  OAUTH2_GOOGLE_CLIENT_ID=prod-client-id
  OAUTH2_GOOGLE_CLIENT_SECRET=prod-client-secret
  ```
- [ ] OR update `application-prod.properties`:
  ```
  spring.security.oauth2.client.registration.google.client-id=prod-id
  spring.security.oauth2.client.registration.google.client-secret=prod-secret
  ```

---

## GitHub OAuth2 Setup (Production)

### Step 1: Update GitHub OAuth App
- [ ] Go to GitHub Settings → Developer settings → OAuth Apps
- [ ] Edit existing app or create new one
- [ ] Update Homepage URL: `https://yourdomain.com`
- [ ] Update Authorization callback URL:
  ```
  https://api.yourdomain.com/login/oauth2/code/github
  ```
- [ ] Save
- [ ] Verify Client ID and Client Secret
- [ ] Note: Client Secret will be shown only once

### Step 2: Configure Backend
- [ ] Add to `.env`:
  ```
  OAUTH2_GITHUB_CLIENT_ID=prod-client-id
  OAUTH2_GITHUB_CLIENT_SECRET=prod-client-secret
  ```
- [ ] OR update `application-prod.properties`:
  ```
  spring.security.oauth2.client.registration.github.client-id=prod-id
  spring.security.oauth2.client.registration.github.client-secret=prod-secret
  ```

---

## Backend Deployment

### Server Configuration
- [ ] Update `application-prod.properties`:
  ```properties
  app.base-url=https://yourdomain.com
  server.ssl.enabled=true
  server.ssl.key-store=/path/to/keystore.jks
  server.ssl.key-store-password=password
  ```
- [ ] Configure CORS for production domain:
  ```java
  configuration.setAllowedOrigins(Arrays.asList(
    "https://yourdomain.com",
    "https://www.yourdomain.com"
  ));
  ```
- [ ] Set `SPRING_PROFILES_ACTIVE=prod` environment variable

### Build & Deploy
- [ ] Run tests: `mvn test`
- [ ] Build package: `mvn clean package -DskipTests -P prod`
- [ ] Verify WAR/JAR file created
- [ ] Upload to production server
- [ ] Start backend with production profile
- [ ] Verify startup logs for errors
- [ ] Test OAuth2 endpoints:
  ```bash
  curl -L https://api.yourdomain.com/oauth2/authorization/google
  curl -L https://api.yourdomain.com/oauth2/authorization/github
  ```

### Database Verification
- [ ] Connect to production database
- [ ] Verify User table exists
- [ ] Verify migrations have run
- [ ] Check for any connection errors

---

## Frontend Deployment

### Build
- [ ] Update `environment.prod.ts` with production API URL
- [ ] Run build: `npm run build --prod`
- [ ] Verify `dist/` folder created
- [ ] Check for any build warnings/errors

### Deploy
- [ ] Upload `dist/` contents to web server
- [ ] Configure web server to serve `index.html` for all routes (SPA)
- [ ] Enable GZIP compression
- [ ] Enable browser caching
- [ ] Set up SSL/HTTPS redirect

### Testing
- [ ] Navigate to https://yourdomain.com/auth/login
- [ ] Verify Google button loads
- [ ] Verify GitHub button loads
- [ ] Click Google button and test flow
- [ ] Click GitHub button and test flow
- [ ] Check DevTools Console for errors
- [ ] Verify token stored in localStorage
- [ ] Verify user created in database

---

## Security Checks

### Credentials
- [ ] Never commit credentials to git
- [ ] Use environment variables for all secrets
- [ ] Rotate secrets periodically
- [ ] Document secret management process
- [ ] Limit access to production environment

### HTTPS
- [ ] SSL certificate valid and not expired
- [ ] All HTTP requests redirect to HTTPS
- [ ] Certificate covers yourdomain.com and subdomains
- [ ] HSTS headers configured

### CORS
- [ ] Only allow production frontend domain
- [ ] Don't allow localhost in production
- [ ] Verify credentials are required for cross-origin requests

### Database
- [ ] Database credentials in environment variables
- [ ] SSL connection to database
- [ ] Database backups automated
- [ ] Sensitive data encrypted

### Logging
- [ ] Enable security logging
- [ ] Monitor authentication failures
- [ ] Monitor for suspicious activity
- [ ] Keep logs for audit trail

---

## Post-Deployment Testing

### Functionality Tests
- [ ] Login with Google works end-to-end
- [ ] Login with GitHub works end-to-end
- [ ] User created in database
- [ ] User can access protected pages
- [ ] Logout clears session
- [ ] Token refresh works (if implemented)

### Performance Tests
- [ ] OAuth2 flow completes in < 10 seconds
- [ ] No console errors
- [ ] Check backend response times
- [ ] Monitor server CPU/memory usage

### Integration Tests
- [ ] Existing login still works (if not OAuth2 only)
- [ ] API calls with Bearer token work
- [ ] Error pages display correctly
- [ ] User profile data displays correctly

### Security Tests
- [ ] Token cannot be accessed from another browser
- [ ] Expired tokens are rejected
- [ ] Invalid tokens are rejected
- [ ] CORS prevents unauthorized requests

---

## Monitoring & Maintenance

### Ongoing Tasks
- [ ] Monitor OAuth2 authentication success rate
- [ ] Monitor failed authentication attempts
- [ ] Check for security advisories
- [ ] Update dependencies regularly
- [ ] Review access logs
- [ ] Monitor disk space and database size

### Error Handling
- [ ] Set up alerts for critical errors
- [ ] Log all OAuth2 failures
- [ ] Document error responses
- [ ] Have recovery plan for outages

### User Communication
- [ ] Document OAuth2 login process for users
- [ ] Provide support contact for issues
- [ ] Monitor for user feedback on OAuth2
- [ ] Track any confusion or problems

---

## Rollback Plan

If issues occur in production:

1. **Immediate Actions**
   - [ ] Disable OAuth2 redirect buttons in frontend
   - [ ] Set backend to return 503 Service Unavailable for OAuth2 endpoints
   - [ ] Notify users

2. **Investigation**
   - [ ] Check backend logs for errors
   - [ ] Verify OAuth2 credentials are correct
   - [ ] Verify redirect URIs match exactly
   - [ ] Check database connectivity

3. **Temporary Workaround**
   - [ ] Existing authentication methods still available
   - [ ] Direct users to alternative login methods
   - [ ] Provide status updates

4. **Full Rollback**
   - [ ] Restore previous backend version
   - [ ] Restore previous frontend version
   - [ ] Verify existing authentication works
   - [ ] Communicate status to users

---

## Documentation

- [ ] Update README with OAuth2 information
- [ ] Document production credentials location (securely)
- [ ] Document backup/recovery procedures
- [ ] Document support contact for OAuth2 issues
- [ ] Create troubleshooting guide for production

---

## Sign-off

- [ ] Backend Lead: _______________ Date: ______
- [ ] Frontend Lead: _______________ Date: ______
- [ ] DevOps/Infrastructure: _______________ Date: ______
- [ ] Security Review: _______________ Date: ______

---

## Post-Launch

**Date Deployed**: __________  
**Deployed By**: __________  
**Version**: __________  

### First Week Monitoring
- [ ] Monitor error rates
- [ ] Check user feedback
- [ ] Verify performance
- [ ] Check security logs

### Follow-up Tasks
- [ ] Schedule security audit in 1 month
- [ ] Plan for token refresh improvement
- [ ] Implement additional OAuth2 providers (optional)
- [ ] Update documentation based on learnings

---

**All items must be checked before production deployment!**

For issues, refer to:
- README_OAUTH2.md
- OAUTH2_TROUBLESHOOTING.md (if available)
- Team contact information
