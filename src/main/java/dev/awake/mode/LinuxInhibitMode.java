package dev.awake.mode;

import java.util.List;

final class LinuxInhibitMode extends ProcessInhibitMode {
    LinuxInhibitMode() {
        super("systemd-inhibit", List.of(
                "systemd-inhibit",
                "--what=idle:sleep",
                "--who=Awake",
                "--why=Awake inhibit mode is active",
                "--mode=block",
                "sleep",
                "infinity"));
    }
}

