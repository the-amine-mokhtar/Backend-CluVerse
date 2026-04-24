# OAuth2 Service Usage Examples

## Using OAuth2 in Your Components

### 1. Initiate OAuth2 Login

```typescript
import { Component } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-my-component',
  template: `
    <button (click)="loginWithGoogle()">Sign in with Google</button>
    <button (click)="loginWithGitHub()">Sign in with GitHub</button>
  `
})
export class MyComponent {
  constructor(private authService: AuthService) {}

  loginWithGoogle(): void {
    this.authService.loginWithOAuth2('google');
  }

  loginWithGitHub(): void {
    this.authService.loginWithOAuth2('github');
  }
}
```

---

### 2. Check if User is Logged In

```typescript
import { Component, OnInit } from '@angular/core';
import { AuthService } from './core/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-protected-page',
  template: `
    <div *ngIf="isLoggedIn">
      <p>Welcome back, {{ getUserInfo().firstName }}!</p>
    </div>
    <div *ngIf="!isLoggedIn">
      <p>Please log in first</p>
      <button (click)="goToLogin()">Go to Login</button>
    </div>
  `
})
export class ProtectedPageComponent implements OnInit {
  isLoggedIn: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
  }

  getUserInfo() {
    return this.authService.getOAuth2UserInfo();
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']);
  }
}
```

---

### 3. Get Current JWT Token

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ApiCallService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  callProtectedApi(url: string) {
    const token = this.authService.getToken();
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get(url, { headers });
  }
}
```

---

### 4. Logout User

```typescript
import { Component } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  template: `
    <nav>
      <button *ngIf="isLoggedIn" (click)="logout()">Logout</button>
    </nav>
  `
})
export class NavbarComponent {
  isLoggedIn: boolean;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.isLoggedIn = this.authService.isLoggedIn();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
```

---

### 5. Display User Profile from OAuth2

```typescript
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-user-profile',
  template: `
    <div class="profile-card">
      <h2>User Profile</h2>
      <div *ngIf="userInfo">
        <p><strong>Email:</strong> {{ userInfo.email }}</p>
        <p><strong>Name:</strong> {{ userInfo.firstName }} {{ userInfo.lastName }}</p>
        <p><strong>User ID:</strong> {{ userInfo.userId }}</p>
      </div>
    </div>
  `,
  styles: [`
    .profile-card {
      border: 1px solid #ddd;
      border-radius: 8px;
      padding: 20px;
      max-width: 400px;
    }
  `]
})
export class UserProfileComponent implements OnInit {
  userInfo: any;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.userInfo = this.authService.getOAuth2UserInfo();
  }
}
```

---

### 6. Conditional Display Based on Login Status

```typescript
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  template: `
    <div *ngIf="isLoggedIn; else notLoggedIn">
      <h1>Welcome, {{ userInfo?.firstName }}!</h1>
      <!-- Your dashboard content -->
    </div>

    <ng-template #notLoggedIn>
      <p>Please log in to view this dashboard</p>
      <button routerLink="/auth/login">Go to Login</button>
    </ng-template>
  `
})
export class DashboardComponent implements OnInit {
  isLoggedIn: boolean = false;
  userInfo: any;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    if (this.isLoggedIn) {
      this.userInfo = this.authService.getOAuth2UserInfo();
    }
  }
}
```

---

### 7. Add OAuth2 Buttons to Custom Pages

Simply import and use `OAuth2ButtonsComponent`:

```typescript
import { Component } from '@angular/core';
import { OAuth2ButtonsComponent } from './features/auth/components/oauth2-buttons/oauth2-buttons.component';

@Component({
  selector: 'app-custom-auth',
  template: `
    <div>
      <h2>Custom Authentication Page</h2>
      <app-oauth2-buttons 
        [title]="'Or continue with'" 
        [showDivider]="true">
      </app-oauth2-buttons>
    </div>
  `,
  imports: [OAuth2ButtonsComponent] // If using standalone components
})
export class CustomAuthComponent {}
```

---

### 8. HTTP Interceptor for Token (Optional)

```typescript
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request);
  }
}

// Add to app.module.ts:
// providers: [
//   { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
// ]
```

---

### 9. Auth Guard for Protected Routes

```typescript
import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    if (this.authService.isLoggedIn()) {
      return true;
    }

    this.router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}

// Use in routing:
// { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] }
```

---

### 10. Refresh Token on Session

```typescript
import { Component, OnInit, OnDestroy } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-session-manager',
  template: ''
})
export class SessionManagerComponent implements OnInit, OnDestroy {
  private sessionCheckSubscription: Subscription;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // Check token every 30 minutes
    this.sessionCheckSubscription = interval(30 * 60 * 1000).subscribe(() => {
      if (this.authService.isLoggedIn()) {
        // Optional: Refresh token if implementing token refresh
        console.log('Session still active');
      }
    });
  }

  ngOnDestroy(): void {
    this.sessionCheckSubscription.unsubscribe();
  }
}
```

---

## Service Methods Reference

### `AuthService` Methods

```typescript
// Login with OAuth2 provider
loginWithOAuth2(provider: 'google' | 'github'): void

// Check if user is logged in
isLoggedIn(): boolean

// Get current JWT token
getToken(): string | null

// Get OAuth2 user info
getOAuth2UserInfo(): {
  email?: string;
  firstName?: string;
  lastName?: string;
  userId?: string;
}

// Logout - clears all stored data
logout(): void
```

---

## LocalStorage Keys

After OAuth2 authentication, the following keys are stored in `localStorage`:

```javascript
localStorage.getItem('token')           // JWT token
localStorage.getItem('userEmail')       // User email
localStorage.getItem('userFirstName')   // User first name
localStorage.getItem('userLastName')    // User last name
localStorage.getItem('userId')          // User ID from backend
```

---

## Examples for Common Scenarios

### Scenario 1: Show Different UI Based on Login Status

```typescript
@Component({
  template: `
    <div *ngIf="authService.isLoggedIn(); else loginPrompt">
      <!-- Show after login -->
      <p>Hello, {{ (authService.getOAuth2UserInfo()).firstName }}!</p>
      <button (click)="logout()">Logout</button>
    </div>

    <ng-template #loginPrompt>
      <!-- Show before login -->
      <p>Please log in to continue</p>
      <app-oauth2-buttons></app-oauth2-buttons>
    </ng-template>
  `
})
export class ConditionalComponent {
  constructor(public authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
```

### Scenario 2: Redirect Non-Authenticated Users

```typescript
@Component({
  template: `<div>Loading...</div>`
})
export class LoginCheckComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
```

---

## Troubleshooting Common Issues

### Issue: Token not persisting after page refresh
**Solution**: localStorage might be cleared by browser. Check:
```typescript
console.log(localStorage.getItem('token'));
```

### Issue: OAuth2 buttons not showing
**Solution**: Make sure `OAuth2ButtonsComponent` is declared in `auth.module.ts`

### Issue: Redirect loop
**Solution**: Clear localStorage manually:
```typescript
localStorage.clear();
```

---

For more examples, see the existing components:
- `OAuth2CallbackComponent` - Handles callback
- `OAuth2ButtonsComponent` - Displays buttons
- `MemberLoginComponent` - Login page example
