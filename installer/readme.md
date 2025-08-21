## 📦 Installer Setup (README – Installer section)

```
## Installer Setup

The installer bundles the application JAR generated from this project.

The JAR file and required third-party installers are NOT included in the repository
and must be provided locally before building the installer.

This design keeps the repository clean and avoids versioning binary artifacts.
```

---

## 🛠 Required Files (Mandatory)

```
The installer script requires the following files to be located in the SAME folder
with the EXACT filenames listed below. The build will fail if the names differ.

Required files:
- nssm.exe
- smartmontools-7.4-1.win32-setup.exe
- ssd-tbw-monitoring-api-0.0.1-SNAPSHOT.jar
- TBW Monitor SCRIPT.txt
```

⚠️ **Important:**
Do not rename any of these files. The NSIS/script logic depends on these exact names.

---

## 🔧 Build Steps

```
1. Build the application JAR:
   mvn clean package

2. Copy the generated JAR from the /target directory into the installer folder:
   target/ssd-tbw-monitoring-api-0.0.1-SNAPSHOT.jar

3. Download and place the required third-party installers in the same folder:
   - NSSM (https://nssm.cc/download)
   - Smartmontools (https://www.smartmontools.org/)

4. Verify that all required files are present in the installer directory.

5. Build the installer using NSIS.
```

---

## 📥 Third-Party Dependencies (Not Included)

```
The following third-party tools are required but NOT included in this repository:

- NSSM (Non-Sucking Service Manager)
  https://nssm.cc/download

- Smartmontools
  https://www.smartmontools.org/
```

---

## 📌 Notes

```
- The final installer (.exe) is published via GitHub Releases.
- The repository only contains source code and installer scripts.
- Binary files (.exe, .jar) are intentionally excluded from version control.
```