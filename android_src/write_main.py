import os
import glob

# Find the destination MainActivity.java
dest_files = glob.glob('android/**/MainActivity.java', recursive=True)
if not dest_files:
    print("ERROR: MainActivity.java not found in android folder")
    exit(1)

dest = dest_files[0]
print(f"Writing to: {dest}")

# Try to find the speech recognition plugin Java file
plugin_java = glob.glob(
    'node_modules/@capacitor-community/speech-recognition/android/**/*.java',
    recursive=True
)

print(f"Found plugin Java files: {plugin_java}")

plugin_import = None
plugin_class = None

for f in plugin_java:
    content = open(f).read()
    if 'CapacitorPlugin' in content or 'Plugin' in content:
        lines = content.split('\n')
        for line in lines:
            if line.strip().startswith('package '):
                pkg = line.strip().replace('package ', '').replace(';', '').strip()
            if line.strip().startswith('public class') and 'Plugin' in line:
                cls = line.strip().split()[2]
                plugin_import = f"{pkg}.{cls}"
                plugin_class = cls
                print(f"Found plugin: {plugin_import}")
                break
        if plugin_class:
            break

# Build the MainActivity content
if plugin_class:
    content = f"""package com.nawaz.fixlog;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import {plugin_import};

public class MainActivity extends BridgeActivity {{
    @Override
    public void onCreate(Bundle savedInstanceState) {{
        registerPlugin({plugin_class}.class);
        super.onCreate(savedInstanceState);
    }}
}}
"""
    print(f"Using plugin class: {plugin_class}")
else:
    # No plugin found - use basic MainActivity (app still works, keyboard mic available)
    content = """package com.nawaz.fixlog;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
"""
    print("WARNING: Plugin class not found, using basic MainActivity")

# Write the file
with open(dest, 'w') as f:
    f.write(content)

print("=== Written MainActivity.java ===")
print(open(dest).read())
print("Done.")
