# Google Play Store Approval & Compliance Guide for CodX Controller

This document provides the exact questions, field entries, and declarations required in the **Google Play Console** to ensure your **CodX Controller** app passes review without rejection.

---

## 1. Google Play Developer Policies Critical for CodX Controller

Google Play enforces strict scrutiny on apps involving remote control, device monitoring, or audio. Your app complies with all of them as follows:

| Policy Area | Google Play Requirement | How CodX Controller Complies |
| :--- | :--- | :--- |
| **Stalkerware / Spyware Policy** | Apps must NOT be presented as a surveillance or secret monitoring solution. | The app is documented and marketed as an authorized remote controller for hardware meetings/lectures. Pairing requires an explicit physical 6-digit code on Phone A. Phone A displays a persistent, non-dismissible notification while recording. |
| **Account Deletion Policy** | Apps supporting account creation must allow users to delete their account and data both **in-app** and via a **public web link**. | An in-app "Delete Account & Associated Data" button is located in the User Profile sheet, and the backend provides `DELETE /api/auth/delete-account`. A web portal URL is provided. |
| **User Data & Data Safety** | All collected data types must be accurately declared in the Data Safety form. | Exact answers for Google Play Console Data Safety form are detailed in Section 3 below. |
| **Audio Recording Permission** | CodX Controller does not record audio; only Phone A (Recorder) does. Phone B streams or downloads. | Phone B does NOT request `RECORD_AUDIO`. Phone B only requires `INTERNET` and `ACCESS_NETWORK_STATE`. |

---

## 2. In-App Prominent Disclosure & Consent Screen

Google Play requires a **Prominent Disclosure** before a user pairs a device or accesses remote features. CodX Controller displays this prominently on the Login & Pairing screens:

> **"Prominent Disclosure:** CodX Controller is a remote companion application designed solely for managing recording timetables on devices that you own or have explicit authorization to control. Both devices must be explicitly paired using the on-screen physical 6-digit code. The paired recorder will always display a persistent status notification during active recording sessions. Covert monitoring without mutual consent is strictly prohibited."

---

## 3. Google Play Console — Data Safety Form Answers

When completing the **Data safety** questionnaire in the Google Play Console, use the following exact answers:

### Data Collection & Security
- **Does your app collect or share any of the required user data types?** -> **Yes**
- **Is all of the user data collected by your app encrypted in transit?** -> **Yes** (All requests use TLS/HTTPS)
- **Do you provide a way for users to request that their data be deleted?** -> **Yes**
- **Add a link that users can use to request their data be deleted:** -> `https://codx-audio.com/delete-account` (or your domain)

---

### Data Types Declaration Table

#### 1. Personal Info
| Field | Selection |
| :--- | :--- |
| **Name** | Collected: **Yes** |
| | Shared: **No** |
| | Ephemeral processing: **No** |
| | Required or Optional: **Required** (via Google Sign-In) |
| | Purpose: **App functionality, Account management** |
| **Email address** | Collected: **Yes** |
| | Shared: **No** |
| | Ephemeral processing: **No** |
| | Required or Optional: **Required** (Account identity) |
| | Purpose: **App functionality, Account management** |
| **User IDs (Firebase UID)** | Collected: **Yes** |
| | Shared: **No** |
| | Required or Optional: **Required** |
| | Purpose: **App functionality, Account management** |

#### 2. Audio Files
| Field | Selection |
| :--- | :--- |
| **Voice or sound recordings** | Collected: **Yes** (Retrieved from cloud server for playback) |
| | Shared: **No** (Stored privately in user's isolated account) |
| | Ephemeral processing: **No** |
| | Required or Optional: **Optional** (Generated only when timetables execute) |
| | Purpose: **App functionality** (Audio review & playback) |

#### 3. Device or Other IDs
| Field | Selection |
| :--- | :--- |
| **Device or other IDs** | Collected: **Yes** (Client generated hardware UUID) |
| | Shared: **No** |
| | Purpose: **App functionality, Device pairing** |

---

## 4. App Access & Review Instructions for Google Testers

Under **App Content -> App access** in the Play Console, choose **"All or some functionality is restricted"** and provide the following test credentials:

```
Instructions for Google Review Team:
1. Launch CodX Controller.
2. Tap "Sign in with Google" and sign in with the test Google account.
3. In the Pairing tab, tap "Use Demo Recorder" or enter Demo Code: CDX-DEMO to pair with the pre-configured test environment.
4. Server URL is pre-configured to https://demo.codx-audio.com/ (or your live server).
5. You can view schedules, test timetable creation, and stream sample test recordings in the built-in audio player.
```

---

## 5. Privacy Policy & Terms of Service URLs

In Google Play Console under **App Content -> Privacy Policy**:
- **Privacy Policy URL**: `https://codx-audio.com/privacy-policy` (Upload [PRIVACY_POLICY.md](file:///C:/Users/itsan/.gemini/antigravity/scratch/codx-project/PRIVACY_POLICY.md) to your website or GitHub Pages).
- **Terms of Service**: Host [TERMS_AND_CONDITIONS.md](file:///C:/Users/itsan/.gemini/antigravity/scratch/codx-project/TERMS_AND_CONDITIONS.md) alongside the privacy policy.
