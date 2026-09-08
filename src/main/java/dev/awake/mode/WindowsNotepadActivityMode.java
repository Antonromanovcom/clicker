package dev.awake.mode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class WindowsNotepadActivityMode extends AbstractLifecycleMode {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String PULSE_SCRIPT =
            "$ErrorActionPreference='Stop';"
            + "$pidExpected=[int]$args[0];$file=$args[1];$expected=[int]$args[2];$erase=[int]$args[3];"
            + "Add-Type -TypeDefinition 'using System;using System.Runtime.InteropServices;public static class AwakeUser32{"
            + "[DllImport(\"user32.dll\")]public static extern IntPtr GetForegroundWindow();"
            + "[DllImport(\"user32.dll\")]public static extern uint GetWindowThreadProcessId(IntPtr h,out uint p);"
            + "[DllImport(\"user32.dll\")]public static extern bool SetForegroundWindow(IntPtr h);}';"
            + "$previous=[AwakeUser32]::GetForegroundWindow();"
            + "try{$process=Get-Process -Id $pidExpected;"
            + "if([IO.Path]::GetFileName($process.Path) -ine 'notepad.exe'){throw 'Target process is not Notepad'};"
            + "$shell=New-Object -ComObject WScript.Shell;"
            + "if(-not $shell.AppActivate($pidExpected)){throw 'Windows refused to activate the target Notepad window'};"
            + "Start-Sleep -Milliseconds 200;$current=[AwakeUser32]::GetForegroundWindow();[uint32]$frontPid=0;"
            + "[void][AwakeUser32]::GetWindowThreadProcessId($current,[ref]$frontPid);"
            + "if($frontPid -ne $pidExpected){throw 'Foreground process verification failed'};"
            + "$before=[IO.File]::ReadAllText($file);if($before.Length -ne $expected){throw 'Temporary file changed unexpectedly'};"
            + "$shell.SendKeys('^{END}x^s');Start-Sleep -Milliseconds 250;"
            + "$after=[IO.File]::ReadAllText($file);if($after.Length -ne ($expected+1) -or -not $after.EndsWith('x')){throw 'Typed character was not confirmed'};"
            + "$newCount=$expected+1;if($erase -gt 0 -and $newCount -eq $erase){"
            + "$keys=(('{BACKSPACE}'*$erase) -join '');$shell.SendKeys($keys+'^s');Start-Sleep -Milliseconds 250;"
            + "if(([IO.File]::ReadAllText($file)).Length -ne 0){throw 'Batch deletion was not confirmed'};$newCount=0};"
            + "Write-Output $newCount}finally{if($previous -ne [IntPtr]::Zero){[void][AwakeUser32]::SetForegroundWindow($previous)}}";

    private final Path notepadExecutable;
    private final Long eraseAfter;
    private Path temporaryFile;
    private Path pulseScript;
    private Process notepad;
    private int confirmedCharacters;

    WindowsNotepadActivityMode(Path notepadExecutable, Long eraseAfter) {
        this.notepadExecutable = notepadExecutable;
        this.eraseAfter = eraseAfter;
    }

    @Override
    public String description() {
        return "Windows Notepad activity in an isolated temporary file ("
                + (eraseAfter == null ? "no batch deletion" : "erase each batch of " + eraseAfter) + ")";
    }

    @Override
    protected void onStart() throws ModeException {
        validateExecutable();
        try {
            temporaryFile = Files.createTempFile("awake-activity-", ".txt");
            pulseScript = Files.createTempFile("awake-activity-", ".ps1");
            Files.writeString(pulseScript, PULSE_SCRIPT, StandardCharsets.UTF_8);
            notepad = new ProcessBuilder(notepadExecutable.toString(), temporaryFile.toString()).start();
            long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
            while (System.nanoTime() < deadline && notepad.isAlive()) {
                if (notepad.toHandle().info().command().isPresent()) {
                    Thread.sleep(150);
                    return;
                }
                Thread.sleep(100);
            }
            throw new ModeException("Notepad did not remain available as the launched target process.");
        } catch (IOException exception) {
            throw new ModeException("Could not create the Windows activity workspace.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModeException("Interrupted while starting Notepad.", exception);
        }
    }

    @Override
    public void pulse() throws ModeException {
        if (notepad == null || !notepad.isAlive()) {
            throw new ModeException("The target Notepad process was closed.");
        }
        String result = run(List.of(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", pulseScript.toString(),
                Long.toString(notepad.pid()), temporaryFile.toString(),
                Integer.toString(confirmedCharacters), eraseAfter == null ? "0" : eraseAfter.toString()));
        try {
            confirmedCharacters = Integer.parseInt(result.trim());
        } catch (NumberFormatException exception) {
            throw new ModeException("Windows activity returned an unexpected confirmation.", exception);
        }
    }

    @Override
    protected void onStop() throws ModeException {
        ModeException failure = null;
        if (notepad != null && notepad.isAlive()) {
            notepad.destroy();
            try {
                if (!notepad.waitFor(3, TimeUnit.SECONDS)) notepad.destroyForcibly();
            } catch (InterruptedException exception) {
                notepad.destroyForcibly();
                Thread.currentThread().interrupt();
                failure = new ModeException("Interrupted while closing Notepad.", exception);
            }
        }
        try {
            if (temporaryFile != null) Files.deleteIfExists(temporaryFile);
            if (pulseScript != null) Files.deleteIfExists(pulseScript);
        } catch (IOException exception) {
            ModeException deletion = new ModeException("Could not delete a Windows activity temporary file.", exception);
            if (failure == null) failure = deletion; else failure.addSuppressed(deletion);
        } finally {
            notepad = null;
            temporaryFile = null;
            pulseScript = null;
            confirmedCharacters = 0;
        }
        if (failure != null) throw failure;
    }

    private void validateExecutable() throws ModeException {
        Path name = notepadExecutable.getFileName();
        if (name == null || !name.toString().equalsIgnoreCase("notepad.exe")) {
            throw new ModeException("Windows activity currently supports only a path to notepad.exe.");
        }
        if (!Files.isRegularFile(notepadExecutable)) {
            throw new ModeException("The supplied notepad.exe path does not exist.");
        }
    }

    private String run(List<String> command) throws ModeException {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new ModeException("Windows activity command timed out.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) throw new ModeException("Windows activity failed: " + output);
            return output;
        } catch (IOException exception) {
            throw new ModeException("Could not execute Windows activity automation.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModeException("Windows activity automation was interrupted.", exception);
        }
    }
}
