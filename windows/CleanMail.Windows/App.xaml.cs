using CleanMail.Windows.Services;
using CleanMail.Windows.TrayIcon;
using Microsoft.UI.Xaml;

namespace CleanMail.Windows;

public partial class App : Application
{
    private MainWindow? _mainWindow;
    private TrayIconManager? _trayIcon;
    private SyncBackgroundService? _syncService;

    public static App Instance => (App)Current;
    public ImapService ImapService { get; } = new();
    public SmtpService SmtpService { get; } = new();
    public CryptoService CryptoService { get; } = new();

    public App()
    {
        InitializeComponent();
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        _mainWindow = new MainWindow();
        _trayIcon = new TrayIconManager(_mainWindow);
        _syncService = new SyncBackgroundService(ImapService);
        _syncService.Start();
        _mainWindow.Activate();
    }

    public void ShowMainWindow()
    {
        _mainWindow ??= new MainWindow();
        _mainWindow.Activate();
    }

    public async Task ShutdownAsync()
    {
        if (_syncService is not null) await _syncService.StopAsync();
        _trayIcon?.Dispose();
        _mainWindow?.Close();
    }
}
