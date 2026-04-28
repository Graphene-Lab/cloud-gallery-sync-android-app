# Privacy Policy

Effective date: [Month Day, Year]

This Privacy Policy explains how [Developer / Company Name] ("we", "us", "our") handles information in the following products and related services:
- Graphene Cloud Client desktop app
- Graphene Photos for mobile app
- Graphene Explorer mobile app


## 1. Scope

This policy applies to information handled through the apps listed above, related synchronization services, authentication endpoints, subscription endpoints, and support channels.

Some products can connect to a private cloud, self-hosted server, or cloud operator chosen by you or your organization. When that happens, the operator of that connected cloud may process your files and account data under its own policies. This policy covers our apps and services, not third-party systems we do not control.

## 2. Information We Collect

### 2.1 Information you provide directly or authorize through a sign-in or subscription flow

Depending on the product you use, we may collect:

- account and profile information you provide directly or authorize through sign-in (email address);
- subscription and billing information, such as selected plan, storage amount, subscription period, payment intent data, payment status;
- support or business communications you send to us.

Where a product uses a third-party payment interface, payment card details are generally collected by the payment processor through its own payment form rather than through a custom card form built by us.

### 2.2 Files, folders, and media you choose to sync, upload, back up, import, export, download

We may process:

- file and folder contents;
- photos, videos, and other media you choose to sync or manage;
- related metadata such as file names, folder structure, paths, MIME types, sizes, timestamps, storage usage, sync status, and queue state.

File contents are encrypted on your device before transmission. We receive and store only encrypted content and do not have the ability to decrypt your files, access your encryption keys, or read your files in plaintext.

### 2.3 Security, authentication, and connection data

We may process data needed to authenticate you, connect your device to your cloud, and protect content, such as:

- session state and login status;
- QR-based connection data;
- server identifiers;
- public/private keys, passphrase-derived keys, encryption settings, device keys, and similar security material.
- access tokens and refresh tokens (used to authenticate your session and, only when you sign in via email, to retrieve your QR connection code and PIN);

This data is stored exclusively on your device or computer. Encryption keys, passphrase-derived keys, and private keys never leave your device and are never transmitted to or accessible by us. Where supported by your operating system, sensitive security material is stored using encrypted local storage or OS-provided secure storage (such as the system keychain). If you choose to register or sign in using an email address and password, a QR code and PIN gets stored on our servers solely to deliver connection credentials to your account. This does not include your encryption keys or file content.

### 2.4 Technical, device, and operational data

We may process technical data needed to operate the apps, such as:

- operating system and app version information;
- local configuration and preferences;
- network state, sync progress, and service status;
- recent items, upload/download queue state, onboarding state, or auto-start preferences;
- error and diagnostic information where a product includes crash or diagnostics reporting.

All items listed above are processed and stored locally on your device. The only data that may be transmitted externally is crash and diagnostic information, and only in products where such reporting is explicitly enabled.

## 3. How We Use Information

We use information to:

- provide login, pairing, sync, backup, restore, file management, sharing, and subscription features;
- operate security features, including encrypted storage, QR pairing, passphrase-based recovery, and zero-knowledge encryption where supported by the product;
- process plan selection, payment initiation, subscription provisioning, and account status;
- show sync progress, notifications, and device-specific functionality you enable;
- maintain service reliability, diagnose app errors, prevent abuse, and improve app stability, using diagnostic data only where crash reporting is enabled in the product;
- comply with legal obligations and enforce our terms.

## 4. When We Share Information

We may share information:

- With service providers that help us operate identity, hosting, subscription, payment, diagnostics, customer support, or similar functions — we share only the minimum data necessary for each provider to perform their function. We do not share your file contents with service providers; as file contents are encrypted client-side before upload, we are technically unable to do so.

- With a self-hosted or connected server of your choice — if you choose to connect to a self-hosted or third-party cloud, your data is transferred to that server in encrypted form only. The operator of that server is responsible for their own data practices.

- If required by law, regulation, legal process, or to protect the rights, safety, or security of our users, apps, or services — because file contents are encrypted client-side and we do not hold your encryption keys, any legally compelled disclosure would be limited to account-level data such as email address and subscription information. We cannot provide your file contents in readable form.

- As part of a business transaction, such as a merger, acquisition, financing, reorganization, or sale of assets.

In all cases, your file contents are never shared with any party. Due to our client-side zero-knowledge encryption architecture, sharing your file contents in readable form is technically impossible — we do not hold your encryption keys and have no ability to decrypt your files.

## 5. Retention and Deletion

We retain information for as long as needed to operate the relevant product, maintain security, comply with legal obligations, resolve disputes, and enforce agreements.

In practice:

- local session data, tokens, QR data, and settings may remain on your device until you log out, clear app data, uninstall the app, or remove the relevant local files;
- synced files and backups stored on your Graphene Cloud are retained in encrypted form until you delete them through the app or your account. Because files are encrypted client-side, we store only encrypted content and cannot access the plaintext.If you connect to a self-hosted or third-party server instead, retention of your data on that server is managed by its operator.
- subscription, transaction, and accounting records may be retained for tax, audit, fraud-prevention, legal, and bookkeeping purposes;
- diagnostic data may be retained according to the relevant provider's retention settings and our operational needs.

## 6. Security

We design our products with security as a foundational principle, not an afterthought. Key measures include:

- **Client-side encryption:** All files are encrypted on your device before upload. We never receive, store, or have the ability to recover your encryption keys.
- **Zero-knowledge architecture:** We cannot access, read, or decrypt your file contents. Our systems store only encrypted data.
- **Zero-trust design:** No party — including us — is treated as trusted. Clients are designed so that no server or intermediary can access plaintext content.
- **Metadata obfuscation:** Filenames are obfuscated and metadata exposure is minimized to reduce visibility into your usage patterns.
- **Strong key derivation:** Unique per-file keys are deterministically derived using hash-based derivation with 512-bit key material, which provides resilience against both classical and quantum-computing threats.
- **Open-source transparency:** Our client code is open source, allowing anyone to independently verify our encryption implementations and security claims.
- **Local security measures:** Sensitive data such as tokens, keys, and credentials are stored using encrypted local storage or OS-provided secure storage where supported.

No method of transmission or storage is completely secure. You are responsible for protecting your devices, passwords, PINs, passphrases, recovery materials, and access to any connected cloud service.

## 7. Your Choices

You have meaningful control over your data and how our products operate:

- **Sign-in method:** Choose whether to sign in with an email account or use QR/pairing flows, depending on the product.
- **File control:** Choose which files, folders, or media to sync, upload, back up, download, share, or delete. You can remove synced content at any time through the app.
- **Encryption and recovery:** Set your own passphrase instead of using an auto-generated one. Your passphrase is the main source for your encryption keys — all of which remain exclusively on your device.
- **Device permissions:** Manage permissions such as media library, notifications, and background activity through your device's settings.
- **Diagnostics:** Where a product includes crash or diagnostic reporting, you may be able to control this through device or app settings.
- **Account and data:** Log out, clear local app data, or delete your account.


## 8. Children's Privacy

The apps are not directed to children under 13, and we do not knowingly collect personal information from children under 13.

## 9. Changes to This Policy

We may update this Privacy Policy from time to time. The updated version will be posted at the policy URL with a revised effective date.

## 10. Contact

Graphene Lab  
[Privacy email address]  
[Website URL]  
[Mailing address, if applicable]

## 11. Product-Specific Terms

### 11.1 Graphene Photos (Android)

In addition to Sections 2 through 7:

- **Account data:** If you sign in through OAuth, the app stores access and refresh tokens locally and may read the email claim from the access token to show account and subscription information. If you use a QR-only login flow, the app may operate without an email address.
- **Media access:** The app can request access to photos, videos, and related media metadata on your device so you can scan, sync, and, if you choose, request deletion of already-synced items.
- **Camera and QR:** The app uses camera access for QR-based connection or authentication flows.
- **Background sync and notifications:** The app can run a foreground sync service and show ongoing notifications while scanning and synchronizing media.
- **Subscription features:** The app can request available subscription plans, create payment intents, and present Stripe's payment interface for paid plans.
- **Media location metadata:** Where available, the app may read media-location metadata embedded in photos or videos to manage synced media. This is not the same as continuous GPS tracking of your device.

### 11.2 Explorer / Cloud Services Mobile App

In addition to Sections 2 through 7:

- **Account data:** If you use the sign-in flow, the app stores access and refresh tokens locally. The implemented OAuth request currently asks for `openid` and `profile` scopes.
- **File handling:** The app can import files using the document picker, receive files shared from other apps, download files locally for offline use or re-sharing, and keep recent-item metadata and upload queue state on the device.
- **Local security data:** The app stores local auth state, client and server identifiers, QR-derived data, public/private keys, encryption preferences, device encryption keys, proxy settings, and related sync state needed to connect to your cloud.
- **Camera and QR:** The app can request camera access to scan QR codes used for connection or authentication.
- **Diagnostics:** The app includes Firebase Crashlytics to record app errors and related diagnostic attributes when failures occur.
- **Subscription features:** The app can create Stripe payment intents and present Stripe's payment sheet for paid plans.
- **Network and transfer preferences:** The app stores whether cellular data may be used for certain transfers and can react to shared-content intents delivered by the operating system.
- **Media location metadata:** Where supported by the operating system and granted by you, the app may access media-location metadata associated with selected media files.

### 11.3 Cloud Client Desktop App

In addition to Sections 2 through 7:

- **File sync and backup:** The desktop client processes the contents and metadata of files and folders in the cloud path you configure, and can maintain backups, versions, and hard-link-based backup structures.
- **Local configuration:** The app stores settings such as cloud path, entry point, virtual disk preference, theme, local UI address/port, and auto-start behavior.
- **QR, passphrase, and keys:** The app supports QR-based pairing, PIN login, passphrase-based account recovery, locally stored QR values, server public keys, and zero-knowledge encryption master keys or related security material needed for encryption and reconnection.
- **Virtual disk and elevated privileges:** If enabled, the product may store data inside a password-protected virtual disk and may require administrator/root privileges for tasks such as file access, clock synchronization, or hard-link-based backups.
- **Subscription provisioning:** In some cloud-provisioning or subscription flows, the related service can process storage size, subscription duration, and optionally a customer email address to send a connection QR code.
