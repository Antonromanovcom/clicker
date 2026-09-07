package dev.awake.mode;

import java.util.List;

final class MacOsInhibitMode extends ProcessInhibitMode {
    MacOsInhibitMode() {
        super("caffeinate", List.of("/usr/bin/caffeinate", "-i"));
    }
}

