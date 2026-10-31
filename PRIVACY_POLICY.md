# Privacy Policy for CodX Controller & CodX Recorder

**Last Updated:** October 31, 2026  
**Effective Date:** October 31, 2026  
**Developer:** CodX Technologies  
**Contact Email:** privacy@codx-audio.com  
**Public Policy URL:** https://codx-audio.com/privacy-policy  

---

## 1. Introduction

CodX Technologies ("we", "us", "our") respects your privacy and is committed to protecting your personal data. This Privacy Policy explains how our mobile applications—**CodX Controller** and **CodX Recorder** (collectively, the "Apps")—as well as our synchronization backend ("Cloud Services"), collect, handle, process, store, and protect your information.

This Privacy Policy complies with:
- **Google Play Developer Program Policies** (including the User Data Policy, Prominent Disclosure & Consent requirements, and Account Deletion Policy)
- **General Data Protection Regulation (GDPR)** (Regulation (EU) 2016/679)
- **California Consumer Privacy Act (CCPA / CPRA)**
- **Children's Online Privacy Protection Act (COPPA)**

---

## 2. Information We Collect

We only collect data necessary to provide remote audio schedule configuration, device monitoring, and secure audio playback between paired devices.

### 2.1 Google Account Data (via Firebase Authentication)
When you sign in to CodX Controller using Google Sign-In, we receive the following profile data provided by Google:
- **Name / Display Name:** Used to personalize your controller interface.
- **Email Address:** Used as your unique account identifier, for authentication token issuance, and for service-critical communication.
- **Profile Photo URL:** Displayed in the in-app profile badge.
- **Firebase User ID (UID):** A persistent, pseudonymous unique identifier used to link your account to your paired recorder devices.

### 2.2 Device & Hardware Telemetry Data
To allow Phone B (Controller) to monitor the operating health of Phone A (Recorder), the CodX Recorder app reports telemetry data to the Cloud Service:
- **Battery Percentage and Charging Status:** (e.g., 85%, AC connected) to prevent unexpected shutdown during recordings.
- **Storage Metrics:** Total disk space and available free space in bytes to ensure sufficient capacity for audio files.
- **Network Status:** Connection type (Wi-Fi, Cellular, Ethernet, Offline) to manage upload queues.
- **Operating State:** Current mode (`IDLE`, `RECORDING`, `SYNCING`).

### 2.3 Audio Recordings and Metadata
- **Audio Files:** Captured by Phone A in MPEG-4 AAC (`.m4a`) format according to your configured timetable schedules.
- **Audio Metadata:** File name, recording duration in milliseconds, file size in bytes, creation timestamp, and cryptographically generated SHA-256 integrity checksums.
- **Recording Timetables:** Label, start time, stop time, days of recurrence, and active toggle state.

### 2.4 Device Identifiers & Pairing Codes
- **Hardware Identifiers:** Randomly generated device UUIDs assigned upon initial app launch.
- **Pairing Codes:** Ephemeral 6-digit codes (e.g., `CDX-6852`) used to bind Phone B to Phone A.

---

## 3. How We Use Your Information

We process collected information strictly for the following operational purposes:
1. **Authentication & Access Control:** Verifying your identity with Google Sign-In and ensuring only you can access your paired recorder devices.
2. **Remote Schedule Management:** Delivering recording timetables configured on Phone B to Phone A.
3. **Audio Streaming & Downloading:** Facilitating HTTP Range-request streaming and direct downloads of audio files from Phone A to Phone B.
4. **Hardware Monitoring:** Displaying battery percentage and disk storage alerts to prevent recording failures.
5. **Deduplication & Integrity:** Using SHA-256 checksums to verify that recorded files are transferred without corruption or duplication.

---

## 4. Legal Bases for Processing (GDPR Compliance)

For users in the European Economic Area (EEA) and United Kingdom (UK), we process your personal data under the following legal bases:
- **Performance of a Contract:** Fulfilling the functionality of the CodX Service according to our Terms of Service.
- **Legitimate Interests:** Operating, securing, and maintaining the integrity and security of our Cloud Services.
- **Consent:** Explicit consent provided by you when authenticating with Google and pairing devices.

---

## 5. Data Sharing and Third-Party Disclosures

**We do NOT sell, rent, monetize, or trade your personal data, audio recordings, or device telemetry to any third party or advertising network.**

We share data only with trusted infrastructure providers required to operate the Service:
- **Google Firebase Authentication / Google Play Services:** For secure OAuth authentication and identity verification.
  - [Google Privacy Policy](https://policies.google.com/privacy)
  - [Firebase Data Processing Terms](https://firebase.google.com/terms/data-processing-terms)
- **Cloud Infrastructure Hosts:** For encrypted storage of audio files and relational database operations. All data is stored in secure, access-controlled data centers with strict encryption.

---

## 6. Data Security and Encryption

We employ industry-standard administrative, physical, and technical safeguards to protect your data:
- **Encryption in Transit:** All communication between CodX apps and our Cloud Servers is strictly encrypted using Transport Layer Security (TLS 1.2 / TLS 1.3 with HTTPS).
- **Integrity Verification:** Every recorded audio file is hashed with SHA-256 before upload and validated by the server upon arrival.
- **Access Isolation:** Database queries and API endpoints enforce strict user ownership checks; a Controller cannot query or stream recordings from a Recorder paired to a different user account.

---

## 7. Data Retention and Account Deletion

### 7.1 Retention Period
- Account profile data is retained as long as your account remains active.
- Audio recordings remain on the cloud server until you delete them or your account is terminated.
- Synchronization event logs are automatically pruned after 90 days.

### 7.2 Right to Deletion (Google Play Account Deletion Policy)
You have the absolute right to delete your account and all associated data at any time:
1. **In-App Deletion:** Open **CodX Controller** -> tap **User Profile** -> tap **Delete Account & Associated Data** -> confirm deletion.
2. **Web-Based Deletion Request:** Visit our public portal at **`https://codx-audio.com/delete-account`** or email **`privacy@codx-audio.com`** with your registered Google email address.

Upon receiving an account deletion request:
- All personal profile information is immediately purged.
- All associated recordings, audio files, schedules, and pairing tokens are permanently erased from our cloud servers within 30 days.

---

## 8. Anti-Spyware & Stalkerware Policy Compliance

In strict adherence to Google Play's Stalkerware Policy:
- CodX Recorder displays a **persistent, non-dismissible notification** whenever recording is in progress (`Recording audio in progress...` with a prominent `Stop Recording` action).
- CodX Recorder cannot run invisibly, covertly, or without an active notification icon in the Android status bar.
- Both devices must be explicitly paired using the physical 6-digit code displayed on the Recorder's screen.
- CodX is not marketed or intended as a child-monitoring, employee-tracking, or covert surveillance tool.

---

## 9. Children's Privacy (COPPA Compliance)

Our Services are not directed to individuals under the age of 13 (or under 16 in the European Union). We do not knowingly collect personal information from children. If you become aware that a child has provided us with personal data, please contact us at `privacy@codx-audio.com`, and we will promptly delete the data.

---

## 10. Your Rights (CCPA / GDPR)

Depending on your geographical location, you have the following rights:
- **Right to Access:** Request a copy of the personal data we hold about you.
- **Right to Rectification:** Request correction of inaccurate information.
- **Right to Erasure ("Right to Be Forgotten"):** Request complete deletion of your data.
- **Right to Data Portability:** Download your audio recordings in standard `.m4a` format.
- **Right to Withdraw Consent:** Disconnect your Google account and unpair devices at any time.

To exercise any of these rights, contact our Data Protection Officer at `privacy@codx-audio.com`.

---

## 11. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect operational, legal, or regulatory modifications. We will notify you of any material changes by updating the "Last Updated" date at the top of this policy and providing prominent in-app notification.

---

## 12. Contact Us

If you have questions, concerns, or requests regarding this Privacy Policy, please reach out to us:

**CodX Technologies Privacy Officer**  
Email: `privacy@codx-audio.com`  
Support: `support@codx-audio.com`  
Website: `https://codx-audio.com`
