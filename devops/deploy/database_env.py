"""Serialize Jenkins bindings for systemd EnvironmentFile on stdout."""
import os
import sys

lines = []
for key in ("DB_URL", "DB_USERNAME", "DB_PASSWORD"):
    value = os.environ[key]
    if not value or any(char in value for char in "\r\n\x00"):
        sys.exit(f"{key} must be nonempty and single-line")
    if key == "DB_URL" and not value.startswith("jdbc:postgresql:"):
        sys.exit("DB_URL must point to PostgreSQL")
    escaped = value.replace("\\", "\\\\").replace('"', '\\"')
    lines.append(f'{key}="{escaped}"')
print("\n".join(lines))
