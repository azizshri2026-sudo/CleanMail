using CleanMail.Windows.Services;

namespace CleanMail.Windows.TrayIcon;

/// <summary>
/// Polls IMAP for new messages in the background at a configurable interval.
/// Runs on a dedicated thread — does not block the UI thread.
/// </summary>
public sealed class SyncBackgroundService
{
    private readonly ImapService _imap;
    private CancellationTokenSource? _cts;
    private Task? _loopTask;

    private static readonly TimeSpan SyncInterval = TimeSpan.FromMinutes(5);

    public event Action<int>? NewMessagesFound;

    public SyncBackgroundService(ImapService imap)
    {
        _imap = imap;
    }

    public void Start()
    {
        _cts = new CancellationTokenSource();
        _loopTask = Task.Run(() => RunLoopAsync(_cts.Token));
    }

    public async Task StopAsync()
    {
        _cts?.Cancel();
        if (_loopTask is not null)
        {
            try { await _loopTask.ConfigureAwait(false); }
            catch (OperationCanceledException) { }
        }
    }

    private async Task RunLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            try
            {
                var newMessages = await _imap.SyncInboxAsync();
                if (newMessages.Count > 0)
                    NewMessagesFound?.Invoke(newMessages.Count);
            }
            catch (Exception)
            {
                // Non-fatal: network blip, expired auth, etc.
                // Next iteration will retry.
            }

            try { await Task.Delay(SyncInterval, ct); }
            catch (OperationCanceledException) { break; }
        }
    }
}
