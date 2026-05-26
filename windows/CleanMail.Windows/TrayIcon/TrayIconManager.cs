using Microsoft.UI.Xaml;

namespace CleanMail.Windows.TrayIcon;

/// <summary>
/// Manages the system-tray icon using WinForms NotifyIcon (available in .NET 8 on Windows).
/// Keeps the app running in the background when the main window is closed.
/// </summary>
public sealed class TrayIconManager : IDisposable
{
    private readonly Window _mainWindow;
    private System.Windows.Forms.NotifyIcon? _notifyIcon;
    private bool _isMainWindowVisible = true;

    public TrayIconManager(Window mainWindow)
    {
        _mainWindow = mainWindow;
        InitTrayIcon();

        // Minimize to tray instead of closing
        _mainWindow.Closed += (_, _) =>
        {
            if (_notifyIcon is not null)
                _isMainWindowVisible = false;
        };
    }

    private void InitTrayIcon()
    {
        _notifyIcon = new System.Windows.Forms.NotifyIcon
        {
            Text = "CleanMail",
            Visible = true,
            ContextMenuStrip = BuildContextMenu()
        };

        // Try loading icon from assets; fall back to default app icon
        try
        {
            var iconPath = Path.Combine(AppContext.BaseDirectory, "Assets", "tray.ico");
            if (File.Exists(iconPath))
                _notifyIcon.Icon = new System.Drawing.Icon(iconPath);
        }
        catch { /* non-fatal */ }

        _notifyIcon.DoubleClick += (_, _) => ShowMainWindow();
    }

    private System.Windows.Forms.ContextMenuStrip BuildContextMenu()
    {
        var menu = new System.Windows.Forms.ContextMenuStrip();

        var showItem = new System.Windows.Forms.ToolStripMenuItem("Open CleanMail");
        showItem.Click += (_, _) => ShowMainWindow();

        var quitItem = new System.Windows.Forms.ToolStripMenuItem("Quit");
        quitItem.Click += async (_, _) => await App.Instance.ShutdownAsync();

        menu.Items.Add(showItem);
        menu.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        menu.Items.Add(quitItem);
        return menu;
    }

    public void ShowUnreadBadge(int count)
    {
        if (_notifyIcon is null) return;
        _notifyIcon.Text = count > 0 ? $"CleanMail ({count} unread)" : "CleanMail";
    }

    private void ShowMainWindow()
    {
        _isMainWindowVisible = true;
        App.Instance.ShowMainWindow();
    }

    public void Dispose()
    {
        _notifyIcon?.Dispose();
    }
}
