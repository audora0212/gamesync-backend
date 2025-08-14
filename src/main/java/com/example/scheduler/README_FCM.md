# FCM Backend Setup

Required application properties:

```
firebase.service-account-json={... full JSON of service account ...}
```

Notes:
- Use a dedicated Firebase service account with Messaging permission.
- The property value can be plain JSON or base64-decoded at runtime (this config expects plain JSON string).
- For production, inject via environment variable and Spring's property binding.






