using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using NAudio.CoreAudioApi;

namespace ReceiverWinUI.Core;

public static class DriverInstaller
{
    public static bool IsVbCableInstalled()
    {
        using var enumerator = new MMDeviceEnumerator();
        var devices = enumerator.EnumerateAudioEndPoints(DataFlow.Render, DeviceState.Active);
        return devices.Any(d => d.FriendlyName.Contains("CABLE Input", StringComparison.OrdinalIgnoreCase));
    }

    public static (bool Ok, string Message) Install(Action<string>? log = null)
    {
        log ??= _ => { };

        var root = ResolveAssetsRoot();
        if (root is null)
        {
            return (false, "No VB-CABLE package found. Place installer under assets/vbcable.");
        }

        var candidates = FindInstallerCandidates(root).ToList();
        if (candidates.Count == 0)
        {
            return (false, $"No installer candidates in {root}. Put VBCABLE_Setup_x64.exe or VBCABLE_Driver_Pack*.zip.");
        }

        log($"VB-CABLE candidates: {string.Join(", ", candidates.Select(Path.GetFileName))}");

        var tempDir = Path.Combine(Path.GetTempPath(), $"fluxmic_vbcable_{Guid.NewGuid():N}");
        Directory.CreateDirectory(tempDir);

        try
        {
            var source = candidates[0];
            string? installerPath;

            if (source.EndsWith(".zip", StringComparison.OrdinalIgnoreCase))
            {
                log($"Extracting zip: {Path.GetFileName(source)}");
                ZipFile.ExtractToDirectory(source, tempDir, overwriteFiles: true);
                installerPath = FindInstallerExe(tempDir);
            }
            else
            {
                installerPath = Path.Combine(tempDir, Path.GetFileName(source));
                File.Copy(source, installerPath, overwrite: true);
            }

            if (string.IsNullOrWhiteSpace(installerPath) || !File.Exists(installerPath))
            {
                return (false, "Could not locate x64 installer executable.");
            }

            var run = RunElevatedInstaller(installerPath);
            if (!run.Ok)
            {
                return run;
            }

            for (var i = 0; i < 8; i++)
            {
                if (IsVbCableInstalled())
                {
                    return (true, "VB-CABLE detected.");
                }

                Thread.Sleep(1000);
            }

            return (false, "Installer finished, but VB-CABLE is still not detected. Try rebooting Windows.");
        }
        finally
        {
            try
            {
                Directory.Delete(tempDir, recursive: true);
            }
            catch
            {
                // ignore cleanup failures
            }
        }
    }

    public static string InstallerPathHint()
    {
        var root = ResolveAssetsRoot() ?? Path.Combine(AppContext.BaseDirectory, "assets", "vbcable");
        return Path.Combine(root, "VBCABLE_Setup_x64.exe");
    }

    private static string? ResolveAssetsRoot()
    {
        var candidates = new[]
        {
            Path.Combine(AppContext.BaseDirectory, "assets", "vbcable"),
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "assets", "vbcable")),
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "assets", "vbcable")),
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "assets", "vbcable")),
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "assets", "vbcable"))
        };

        return candidates.FirstOrDefault(Directory.Exists);
    }

    private static IEnumerable<string> FindInstallerCandidates(string root)
    {
        foreach (var path in Directory.GetFiles(root, "VBCABLE_Setup_x64.exe", SearchOption.AllDirectories))
        {
            yield return path;
        }

        foreach (var path in Directory.GetFiles(root, "VBCABLE_Driver_Pack*.zip", SearchOption.AllDirectories))
        {
            yield return path;
        }

        foreach (var path in Directory.GetFiles(root, "*.exe", SearchOption.AllDirectories))
        {
            yield return path;
        }

        foreach (var path in Directory.GetFiles(root, "*.zip", SearchOption.AllDirectories))
        {
            yield return path;
        }
    }

    private static string? FindInstallerExe(string root)
    {
        var setup = Directory.GetFiles(root, "*Setup*x64*.exe", SearchOption.AllDirectories).FirstOrDefault();
        if (!string.IsNullOrWhiteSpace(setup))
        {
            return setup;
        }

        return Directory.GetFiles(root, "*.exe", SearchOption.AllDirectories)
            .FirstOrDefault(path => Path.GetFileName(path).Contains("x64", StringComparison.OrdinalIgnoreCase));
    }

    private static (bool Ok, string Message) RunElevatedInstaller(string installerPath)
    {
        try
        {
            var process = Process.Start(new ProcessStartInfo
            {
                FileName = installerPath,
                UseShellExecute = true,
                Verb = "runas"
            });

            if (process is null)
            {
                return (false, "Failed to launch installer process.");
            }

            process.WaitForExit();
            if (process.ExitCode != 0)
            {
                return (false, $"Installer exit code: {process.ExitCode}");
            }

            return (true, "Installer completed.");
        }
        catch (Win32Exception ex) when (ex.NativeErrorCode == 1223)
        {
            return (false, "UAC elevation was canceled by user.");
        }
        catch (Exception ex)
        {
            return (false, $"Installer failed: {ex.Message}");
        }
    }
}
