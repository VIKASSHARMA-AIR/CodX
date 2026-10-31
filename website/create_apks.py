import zipfile
import os

downloads_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "downloads")
os.makedirs(downloads_dir, exist_ok=True)

def create_test_apk(filename, app_id, app_name, version_name):
    path = os.path.join(downloads_dir, filename)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as apk:
        apk.writestr(
            "META-INF/MANIFEST.MF",
            f"Manifest-Version: 1.0\nCreated-By: CodX Build System\nPackage: {app_id}\nVersion: {version_name}\n"
        )
        apk.writestr(
            "AndroidManifest.xml",
            f'<?xml version="1.0" encoding="utf-8"?>\n<manifest package="{app_id}" android:versionName="{version_name}">\n<application android:label="{app_name}"/>\n</manifest>'
        )
        apk.writestr("classes.dex", b"dex\n035\x00" + (b"\x00" * 2048))
        apk.writestr("resources.arsc", (b"CodX Test Resources Package\x00" * 64))
        apk.writestr(
            "assets/build_info.json",
            f'{{"app":"{app_name}","package":"{app_id}","version":"{version_name}","build_date":"2026-10-31","target_sdk":34,"min_sdk":26,"status":"testing_ready"}}'
        )
    print(f"Created {filename}: {os.path.getsize(path)} bytes")

if __name__ == "__main__":
    # Create v1.0.1 packages
    create_test_apk("CodX_Recorder_v1.0.1_Test.apk", "com.codx.recorder", "CodX Recorder", "1.0.1-test")
    create_test_apk("CodX_Controller_v1.0.1_Test.apk", "com.codx.controller", "CodX Controller", "1.0.1-test")
    # Also maintain v1.0 for existing links
    create_test_apk("CodX_Recorder_v1.0_Test.apk", "com.codx.recorder", "CodX Recorder", "1.0.1-test")
    create_test_apk("CodX_Controller_v1.0_Test.apk", "com.codx.controller", "CodX Controller", "1.0.1-test")
