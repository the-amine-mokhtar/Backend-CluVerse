# OAuth2 Redirect URIs - Copier dans Google Console et GitHub

## 🌐 Backend Server URL
- **Local Development**: `http://localhost:8081`
- **Production**: `https://yourdomain.com`

---

## 1️⃣ GOOGLE - Redirect URIs à enregistrer

### Configuration URL
Go to: https://console.cloud.google.com/

1. Select your project
2. Credentials > OAuth 2.0 Client ID (Web Application)
3. Add these Authorized redirect URIs:

```
http://localhost:8081/login/oauth2/code/google
https://yourdomain.com/login/oauth2/code/google
```

### Frontend Callback
After user authenticates, Google will redirect to backend, which will redirect frontend to:

```
http://localhost:4200/auth/oauth2-callback?token=xxx&email=xxx
(production) https://yourdomain.com/auth/oauth2-callback?token=xxx&email=xxx
```

---

## 2️⃣ GITHUB - Redirect URIs à enregistrer

### Configuration URL
Go to: https://github.com/settings/developers

1. Settings > Developer settings > OAuth Apps
2. Create New OAuth App
3. Set Authorization callback URL:

```
http://localhost:8081/login/oauth2/code/github
https://yourdomain.com/login/oauth2/code/github
```

### Frontend Callback
After user authenticates, GitHub will redirect to backend, which will redirect frontend to:

```
http://localhost:4200/auth/oauth2-callback?token=xxx&email=xxx
(production) https://yourdomain.com/auth/oauth2-callback?token=xxx&email=xxx
```

---

## ✅ Testing Checklist

### Local Development
- [ ] Backend running: `http://localhost:8081`
- [ ] Frontend running: `http://localhost:4200`
- [ ] OAuth2 credentials added to `.env`
- [ ] Google OAuth2 app created with localhost redirect URI
- [ ] GitHub OAuth2 app created with localhost redirect URI
- [ ] Click "Sign in with Google" on login page
- [ ] Click "Sign in with GitHub" on login page
- [ ] User created in database after OAuth2 login
- [ ] JWT token received and stored in localStorage
- [ ] Redirected to /dashboard

### Production Deployment
- [ ] Backend running on production domain (HTTPS)
- [ ] Frontend running on production domain (HTTPS)
- [ ] OAuth2 credentials updated for production
- [ ] Google OAuth2 app updated with production redirect URI
- [ ] GitHub OAuth2 app updated with production redirect URI
- [ ] SSL certificate valid
- [ ] Test OAuth2 login on production
- [ ] User data persisted in production database

---

## 🔧 Troubleshooting URLs

### Check if backend is running
```
curl http://localhost:8081/api/auth/login
```

### Check if OAuth2 endpoint is accessible
```
curl http://localhost:8081/oauth2/authorization/google
curl http://localhost:8081/oauth2/authorization/github
```

### Check redirects
Google login will redirect to:
```
http://localhost:8081/login/oauth2/code/google?code=AUTH_CODE
```

GitHub login will redirect to:
```
http://localhost:8081/login/oauth2/code/github?code=AUTH_CODE
```

Backend OAuth2SuccessHandler will redirect to:
```
http://localhost:4200/auth/oauth2-callback?token=JWT_TOKEN&email=user@example.com
```

---

## 📝 Notes

- **Redirect URI** must match exactly (including protocol, domain, port, and path)
- **Local testing**: Use exactly `http://localhost:8081` (not 127.0.0.1)
- **Production**: Use HTTPS and your actual domain
- **Client Secret**: Never share or commit to git; use environment variables
- **JWT Token**: Automatically stored in localStorage by OAuth2CallbackComponent

---

## 📚 References

- Google OAuth2: https://developers.google.com/identity/protocols/oauth2
- GitHub OAuth2: https://docs.github.com/en/developers/apps/building-oauth-apps
- Spring Security OAuth2: https://spring.io/projects/spring-security-oauth2-client
