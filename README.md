# Farmer App


```mermaid
sequenceDiagram
    participant User
    participant MobileApp as Mobile App
    participant BackendAPI as Backend API
    participant Database
    participant EmailService as Email Service (Optional)

    title User Registration Flow

    User->>MobileApp: Opens Registration Screen
    User->>MobileApp: Enters details (email, password, name, etc.)
    User->>MobileApp: Taps "Register" button

    activate MobileApp
    MobileApp->>MobileApp: Validate input locally (format, completeness)
    alt Local Validation Fails
        MobileApp->>User: Show validation error message (e.g., "Invalid email format")
    else Local Validation Succeeds
        MobileApp->>+BackendAPI: POST /api/users/register (registration data)

        activate BackendAPI
        BackendAPI->>BackendAPI: Validate data server-side (e.g., password strength)
        BackendAPI->>Database: Check if email already exists
        activate Database
        Database-->>BackendAPI: Query Result (exists or not)
        deactivate Database

        alt Email Already Exists or Server Validation Fails
            BackendAPI-->>-MobileApp: 400 Bad Request / 409 Conflict (Error details)
        else Registration Data Valid
            BackendAPI->>BackendAPI: Hash user password
            BackendAPI->>Database: INSERT new user record (hashed password)
            activate Database
            Database-->>BackendAPI: User record created (Success)
            deactivate Database

            opt Send Verification Email
                BackendAPI->>+EmailService: Request verification email send (user email, token)
                activate EmailService
                EmailService-->>User: Sends verification email
                EmailService-->>-BackendAPI: Email sent confirmation
                deactivate EmailService
            end

            BackendAPI-->>-MobileApp: 201 Created (Success, maybe user ID/token)
        end
        deactivate BackendAPI

        alt Registration Successful
            MobileApp->>MobileApp: Store session token/user data (if applicable)
            MobileApp->>User: Show "Registration Successful" / Navigate to next screen (e.g., Login or "Check Email")
        else Registration Failed (Error from Backend)
            MobileApp->>User: Show specific error message (e.g., "Email already in use")
        end
    end
    deactivate MobileApp

    opt Email Verification Step
        User->>User: Checks email inbox
        User->>BackendAPI: Clicks verification link (GET /api/users/verify?token=...)
        activate BackendAPI
        BackendAPI->>BackendAPI: Validate verification token
        alt Token Valid
            BackendAPI->>Database: UPDATE user status to 'verified'
            activate Database
            Database-->>BackendAPI: User status updated
            deactivate Database
            BackendAPI->>User: Show "Email Verified Successfully" page
        else Token Invalid/Expired
            BackendAPI->>User: Show "Invalid/Expired Verification Link" page
        end
        deactivate BackendAPI
    end
```