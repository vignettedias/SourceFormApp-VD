# 🔥 SourceForm – Secure Cloud Intake Platform

## Overview

SourceForm is an enterprise-inspired secure document intake platform designed to demonstrate modern Android development, cloud-native backend engineering, cybersecurity principles, secure file handling, and defense-in-depth architecture.

The platform enables authenticated users to securely submit documents from an Android application to a hardened cloud backend while enforcing authentication, integrity verification, metadata protection, audit logging, and controlled document access.

Unlike traditional upload systems, SourceForm intentionally prevents direct storage access and instead utilizes backend-controlled signed URL issuance for secure document retrieval.

---

# 🚀 Core Features

## 📱 Android Application

Built using Kotlin and Android SDK.

Features include:

* Modern security-focused user interface
* Hybrid authentication support
* Secure form submission workflow
* Document upload support
* Real-time backend communication
* Session management
* Automatic logout after submission
* Inactivity timeout protection
* Secure token-based communication
* Cloud-integrated workflow

---

## 🔐 Hybrid Authentication System

SourceForm supports multiple authentication methods:

### OAuth Providers

* Google Authentication
* Facebook Authentication
* GitHub Authentication

### Native Authentication

* Backend-managed registration
* JWT-based login
* Secure session persistence

Authentication tokens are verified before any protected operation is allowed.

---

# 📁 Secure Document Submission Pipeline

Every upload passes through a hardened processing pipeline:

```text
User Submission
        ↓
Authentication Verification
        ↓
Input Validation
        ↓
File Validation
        ↓
Integrity Hash Generation
        ↓
Metadata Encryption
        ↓
Audit Logging
        ↓
Private Cloud Storage
        ↓
Firestore Metadata Storage
```

Supported formats:

* PDF
* DOC
* DOCX

---

# 🛡️ Security Architecture

## Defense-in-Depth Design

Security controls exist across every layer of the system.

### Authentication Layer

* Firebase OAuth Verification
* JWT Verification
* Protected API Routes
* Session Validation

### Backend Security

* Helmet Security Headers
* Rate Limiting
* Input Validation
* Error Isolation
* Request Sanitization
* Abuse Prevention

### Upload Security

* MIME Type Validation
* Extension Validation
* Upload Restrictions
* Secure Multipart Processing
* Size Limiting
* Filename Randomization

---

# 🔒 Metadata Encryption

Sensitive submission metadata is encrypted before storage using a custom Fernet-compatible cryptographic implementation.

Protected fields include:

* File metadata
* Submission details
* Storage references
* User-associated information

This reduces metadata exposure even in the event of database compromise.

---

# ✅ SHA-256 Integrity Verification

Every uploaded document receives a cryptographic SHA-256 fingerprint.

Integrity verification allows:

* Tamper detection
* Consistency validation
* Forensic verification
* Secure retrieval validation

The platform can verify whether a stored file remains unchanged from its original uploaded state.

---

# 📜 Audit Logging

Security-relevant events are recorded through an audit subsystem.

Examples include:

* Authentication events
* Upload operations
* Retrieval requests
* Integrity verification events
* Security exceptions

This improves accountability and supports security monitoring.

---

# ☁️ Private Storage Architecture

SourceForm intentionally uses a deny-by-default storage model.

Storage rules are configured to prevent direct public access:

```text
allow read, write: if false;
```

Uploaded documents are not publicly accessible.

This prevents:

* Direct URL access
* Bucket browsing
* Unauthorized downloads
* Security bypasses

---

# 🔗 Signed URL Access Control

Document retrieval follows a secure backend-mediated process:

```text
Authenticated Request
        ↓
Authorization Validation
        ↓
Metadata Retrieval
        ↓
Integrity Verification
        ↓
Audit Logging
        ↓
Signed URL Generation
        ↓
Temporary Secure Access
```

Characteristics:

* Time-limited access
* Backend-generated URLs
* Auditable downloads
* No permanent public links
* Controlled retrieval workflow

---

# ☁️ Cloud Infrastructure

## Firebase

Used for:

* Authentication
* OAuth Providers
* Identity Management
* Token Verification

## Cloud Firestore

Used for:

* Submission Metadata
* Audit Records
* Secure Document References

## Firebase Storage

Used for:

* Private File Storage
* Signed URL Retrieval Model

## Google Cloud Run

Used for:

* Backend Deployment
* API Hosting
* Scalable Request Processing

## MongoDB Atlas

Used for:

* Native Authentication Users
* JWT Authentication Infrastructure

---

# 🔄 Session Security

SourceForm includes active session protection mechanisms:

### Automatic Logout

Users are automatically logged out:

* 5 seconds after successful submission
* After prolonged inactivity

This minimizes session persistence risk.

---

# 🧩 System Architecture

```text
ANDROID APPLICATION
        │
        ▼
Hybrid Authentication Layer
(JWT + OAuth)
        │
        ▼
Retrofit Networking
        │
        ▼
Cloud Run Backend
(Node.js + Express)
        │
 ┌──────┼──────────┐
 ▼      ▼          ▼
Firestore Storage Audit Logs
 ▼
Private Firebase Storage
        │
        ▼
Signed URL Retrieval
```

---

# 📚 Technology Stack

| Category       | Technologies                                 |
| -------------- | -------------------------------------------- |
| Mobile         | Kotlin, Android SDK                          |
| Authentication | Firebase Auth, JWT                           |
| Backend        | Node.js, Express.js                          |
| Database       | MongoDB Atlas, Firestore                     |
| Storage        | Firebase Storage                             |
| Networking     | Retrofit, OkHttp                             |
| Security       | Helmet, Rate Limiting                        |
| Cloud          | Google Cloud Run                             |
| Cryptography   | SHA-256, Custom Fernet-Compatible Encryption |
| OAuth          | Google, Facebook, GitHub                     |

---

# 🏗️ Key Cybersecurity Concepts Demonstrated

* Defense-in-Depth
* Zero Trust Principles
* Private Storage Architecture
* Secure File Handling
* OAuth Authentication
* JWT Authentication
* Audit Logging
* Cryptographic Integrity Verification
* Metadata Protection
* Signed URL Access Control
* Secure Cloud Deployment
* Session Security Controls

---

# 🎯 Educational Objectives

This project was developed as a practical demonstration of:

* Android Application Development
* Secure Backend Engineering
* Cloud Computing
* API Security
* Authentication Systems
* Secure Storage Architectures
* Cryptographic Integrity Controls
* Cybersecurity Fundamentals
* Cloud-Native Deployment Practices

---

# 👨‍💻 Project Context

Developed as part of a cybersecurity-focused full-stack engineering internship project emphasizing secure software design, modern cloud architecture, and practical security engineering principles.

---

# ⭐ License

This project is intended for:

* Educational Use
* Security Research
* Portfolio Demonstrations
* Internship Showcases
* Full-Stack Development Learning
* Cybersecurity Practice
