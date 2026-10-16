package com.cir0cuit.devtoys.ui

object LegalTexts {
    const val REPO_URL = "https://github.com/Cir0cuit/devtoys-android"
    const val ISSUES_URL = "$REPO_URL/issues"

    val LICENSE = """
        Copyright 2026 Bartłomiej Aleksander Smolnik (Cir0cuit)

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.

        Source code: $REPO_URL
    """.trimIndent()

    val PRIVACY_POLICY = """
        DEVTOYS PRIVACY POLICY

        Last Updated: October 2026

        DevToys is an open-source application maintained by Bartłomiej Aleksander Smolnik (Cir0cuit) ("I", "me", or "my"). This page informs you of my policies regarding the collection, use, and disclosure of personal data when you use the application (the "Application").

        1. STRICT "ZERO-DATA" STANCE
        The Application is built upon a fundamental commitment to privacy and data minimization. I do not collect, process, log, or transmit any personal data, usage analytics, telemetry data, or crash reports.

        2. LOCAL PROCESSING ONLY
        All operations and utilities provided by the Application process data strictly and exclusively on your local device. Your input data, configurations, and generated outputs never leave your device and are never routed through my servers or any third-party infrastructure.

        3. NO DATA COLLECTION OR SHARING
        Because the Application does not collect any data, there is absolutely no data sharing, selling, or distribution to any third parties, advertisers, or analytics providers. I do not track your behavior, nor do I build profiles based on your use of the Application.

        4. DEVICE PERMISSIONS
        The Application may request specific device permissions (such as access to the clipboard or local storage) solely to enable the functionality of specific utilities within the Application. These permissions are utilized strictly locally. They are never employed for data extraction, background tracking, or off-device transmission. If you choose to deny these permissions, certain features of the Application may be unavailable, but the core functionalities that do not require such permissions will continue to operate normally.

        5. CHILDREN'S PRIVACY
        My strict zero-data policy applies universally. I do not collect any personally identifiable information from anyone, including children under the age of 13.

        6. CHANGES TO THIS PRIVACY POLICY
        Changes to this Privacy Policy are published in the Application and in the public source repository. Changes are effective when they are published.

        7. CONTACT
        If you have any questions about this Privacy Policy, please open an issue at:
        $ISSUES_URL
    """.trimIndent()
}
