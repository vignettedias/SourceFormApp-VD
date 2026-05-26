# 🔥 SourceForm Secure Cloud Intake System

A modern secure Android + Node.js full-stack application for authenticated form submissions, secure file uploads, backend discovery, and cloud-based storage.

Built using:

* Kotlin Android
* Firebase Authentication
* Node.js + Express.js
* MongoDB Atlas
* Retrofit Networking
* Secure Upload Validation
* Cybersecurity Hardening

---

# 🚀 Features

## 📱 Android Application

* Modern dark-themed UI
* Animated success feedback
* Form validation
* Dynamic backend connection
* QR-based backend discovery
* Secure file uploads
* Firebase OAuth authentication
* Retrofit networking
* Real-time backend status

---

## 🔐 Authentication

Supported login mechanisms:

* Google OAuth
* GitHub OAuth
* Facebook OAuth

Backend verifies Firebase authentication tokens before accepting uploads.

---

## 📁 Secure Upload System

Supports:

* PDF
* DOC
* DOCX

Security protections:

* File extension validation
* MIME type validation
* Executable signature detection
* File signature verification
* Safe randomized filenames
* File size limiting

---

# 🛡️ Cybersecurity Features

## Backend Security

* Helmet security headers
* Rate limiting
* Suspicious activity logging
* Firebase token verification
* Protected API routes
* Input sanitization
* Global error handling

---

## Upload Security

* Executable blocking
* Binary signature inspection
* Upload throttling
* Abuse prevention
* Secure multipart handling

---

# ☁️ Cloud Integration

## Firebase

Used for:

* Authentication
* OAuth providers
* Token management

---

## MongoDB Atlas

Used for:

* Cloud data persistence
* Form submission storage
* Scalable backend database architecture

---

# 🧩 Project Architecture

```text
ANDROID APP
     ↓
Retrofit Networking Layer
     ↓
Firebase Authentication Token
     ↓
Secure Express.js Backend
     ↓
MongoDB Atlas Cloud Database
```

---

# 📚 Technologies Used

| Category        | Technologies          |
| --------------- | --------------------- |
| Mobile          | Kotlin, Android SDK   |
| Backend         | Node.js, Express.js   |
| Database        | MongoDB Atlas         |
| Authentication  | Firebase Auth         |
| Networking      | Retrofit, OkHttp      |
| Upload Handling | Multer                |
| Security        | Helmet, Rate Limiting |
| QR Scanning     | ZXing                 |
| Cloud           | Firebase + MongoDB    |

---

# 📂 Project Structure

```text
SourceForm/

├── app/
│   ├── src/
│   ├── res/
│   └── java/
│
├── Backend/
│   ├── middleware/
│   ├── models/
│   ├── routes/
│   ├── uploads/
│   ├── firebaseAdmin.js
│   ├── server.js
│   └── package.json
```

---

# ⚙️ Setup Instructions

# 1️⃣ Clone Repository

```bash
git clone <repository-url>
```

---

# 2️⃣ Backend Setup

Navigate to backend folder:

```bash
cd Backend
```

Install dependencies:

```bash
npm install
```

Create:

```text
.env
```

Add:

```env
MONGO_URI=your_mongodb_connection_string
```

Place Firebase Admin SDK file:

```text
firebase-service-account.json
```

inside:

```text
Backend/
```

Run backend:

```bash
node server.js
```

---

# 3️⃣ Android Setup

Open project in:

```text
Android Studio
```

Sync Gradle.

Add:

```text
google-services.json
```

inside:

```text
app/
```

Run application.

---

# 📷 Backend QR Discovery

The backend automatically:

* detects local IP
* generates QR code
* allows Android app to dynamically connect

---

# 🔐 Security Highlights

Implemented protections against:

* brute-force attacks
* upload abuse
* malformed requests
* unauthorized uploads
* executable uploads
* token forgery
* API flooding

---

# 🚀 Future Improvements

Planned:

* JWT-based native authentication
* HTTPS deployment
* Certificate pinning
* Cloud deployment
* Admin dashboard
* Malware scanning
* Dockerization
* Cloudflare integration
* Audit logging
* CI/CD pipelines

---

# 📖 Educational Purpose

This project was also built as a:

```text
full-stack cybersecurity + mobile development learning system
```

covering:

* secure backend architecture
* Android networking
* authentication systems
* upload security
* API hardening
* cloud integration
* modern UI/UX

---

# 👨‍💻 Author

Developed as part of:

```text
Task 1 Internship Project
```

Focused on:

* Secure Full-Stack Engineering
* Android Development
* Backend Security
* Cybersecurity Fundamentals

---

# ⭐ License

This project is intended for:

* educational purposes
* portfolio demonstrations
* security learning
* full-stack development practice

```
```
