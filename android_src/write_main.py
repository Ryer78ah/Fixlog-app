import os
import glob

# Find destination MainActivity.java
dest_files = glob.glob('android/**/MainActivity.java', recursive=True)
if not dest_files:
    print("ERROR: MainActivity.java not found")
    exit(1)

dest = dest_files[0]
print(f"Destination: {dest}")

# Search ALL java files in node_modules for the speech plugin
all_java = glob.glob('node_modules/@capacitor-community/speech-recognition/**/*.java', recursive=True)
print(f"All Java files in plugin: {all_java}")

# Also check the android folder inside the capacitor project
synced = glob.glob('android/**/*.java', recursive=True)
print(f"All Java files in android project: {synced}")

# Find the plugin class - search broadly
plugin_pkg = None
plugin_cls = None

# First check if it got synced into the android project already
for f in synced:
    if 'speechrecognition' in f.lower() or 'speech' in f.lower():
        print(f"Found speech file: {f}")
        content = open(f).read()
        print(content[:500])
        for line in content.split('\n'):
            if line.strip().startswith('package '):
                plugin_pkg = line.strip().replace('package ','').replace(';','').strip()
            if 'public class' in line and ('Plugin' in line or 'Recognition' in line):
                plugin_cls = line.strip().split()[2].split('{')[0].strip()
        if plugin_pkg and plugin_cls:
            print(f"Found from synced: {plugin_pkg}.{plugin_cls}")
            break

# If not found in android, check node_modules
if not plugin_cls:
    for f in all_java:
        print(f"Checking: {f}")
        content = open(f).read()
        for line in content.split('\n'):
            if line.strip().startswith('package '):
                plugin_pkg = line.strip().replace('package ','').replace(';','').strip()
            if 'public class' in line and ('Plugin' in line or 'Recognition' in line):
                plugin_cls = line.strip().split()[2].split('{')[0].strip()
        if plugin_pkg and plugin_cls:
            print(f"Found from node_modules: {plugin_pkg}.{plugin_cls}")
            break

print(f"Final: package={plugin_pkg} class={plugin_cls}")

if plugin_pkg and plugin_cls:
    java = (
        "package com.nawaz.fixlog;\n\n"
        "import android.os.Bundle;\n"
        "import com.getcapacitor.BridgeActivity;\n"
        f"import {plugin_pkg}.{plugin_cls};\n\n"
        "public class MainActivity extends BridgeActivity {\n"
        "    @Override\n"
        "    public void onCreate(Bundle savedInstanceState) {\n"
        f"        registerPlugin({plugin_cls}.class);\n"
        "        super.onCreate(savedInstanceState);\n"
        "    }\n"
        "}\n"
    )
else:
    # Hard fallback with known community plugin path
    print("Using hardcoded fallback import")
    java = (
        "package com.nawaz.fixlog;\n\n"
        "import android.os.Bundle;\n"
        "import com.getcapacitor.BridgeActivity;\n"
        "import com.getcapacitor.community.speechrecognition.SpeechRecognition;\n\n"
        "public class MainActivity extends BridgeActivity {\n"
        "    @Override\n"
        "    public void onCreate(Bundle savedInstanceState) {\n"
        "        registerPlugin(SpeechRecognition.class);\n"
        "        super.onCreate(savedInstanceState);\n"
        "    }\n"
        "}\n"
    )

with open(dest, 'w') as f:
    f.write(java)

print("=== MainActivity.java written ===")
print(open(dest).read())
