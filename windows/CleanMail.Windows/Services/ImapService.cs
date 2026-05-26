using CleanMail.Windows.ViewModels;
using MailKit;
using MailKit.Net.Imap;
using MailKit.Security;
using MimeKit;
using System.Net.Sockets;

namespace CleanMail.Windows.Services;

/// <summary>
/// Wraps MailKit IMAP client. All network I/O runs over TLS (port 993 SSL/TLS).
/// Credentials are loaded at call time via <see cref="CryptoService"/> — never cached in memory longer than needed.
/// </summary>
public sealed class ImapService : IDisposable
{
    private readonly CryptoService _crypto = App.Instance.CryptoService;
    private ImapClient? _client;
    private readonly SemaphoreSlim _lock = new(1, 1);

    // Connection details — populated when first account is loaded
    private string _host = "";
    private int _port = 993;
    private string _username = "";

    public void Configure(string host, int port, string username)
    {
        _host = host;
        _port = port;
        _username = username;
    }

    public async Task<IReadOnlyList<EmailListItem>> FetchInboxAsync(int maxMessages = 50)
    {
        var client = await GetConnectedClientAsync();
        var inbox = client.Inbox;
        await inbox.OpenAsync(FolderAccess.ReadOnly);

        int count = inbox.Count;
        int start = Math.Max(0, count - maxMessages);
        var messages = await inbox.FetchAsync(start, count - 1, MessageSummaryItems.UniqueId |
            MessageSummaryItems.Envelope | MessageSummaryItems.Flags);

        return messages
            .OrderByDescending(m => m.Date)
            .Select(ToListItem)
            .ToList();
    }

    public async Task<IReadOnlyList<EmailListItem>> SyncInboxAsync()
    {
        var client = await GetConnectedClientAsync();
        var inbox = client.Inbox;
        await inbox.OpenAsync(FolderAccess.ReadOnly);

        // Fetch only new messages (unseen since last UIDNEXT)
        var uids = await inbox.SearchAsync(MailKit.Search.SearchQuery.NotSeen);
        if (uids.Count == 0) return [];

        var messages = await inbox.FetchAsync(uids, MessageSummaryItems.UniqueId |
            MessageSummaryItems.Envelope | MessageSummaryItems.Flags);

        return messages.Select(ToListItem).ToList();
    }

    public async Task MarkReadAsync(string uniqueId)
    {
        if (!UniqueId.TryParse(uniqueId, out var uid)) return;
        var client = await GetConnectedClientAsync();
        var inbox = client.Inbox;
        await inbox.OpenAsync(FolderAccess.ReadWrite);
        await inbox.AddFlagsAsync(uid, MessageFlags.Seen, silent: true);
    }

    public async Task<string> FetchBodyAsync(string uniqueId)
    {
        if (!UniqueId.TryParse(uniqueId, out var uid))
            return "";
        var client = await GetConnectedClientAsync();
        var inbox = client.Inbox;
        await inbox.OpenAsync(FolderAccess.ReadOnly);
        var msg = await inbox.GetMessageAsync(uid);
        return msg.TextBody ?? msg.HtmlBody ?? "";
    }

    private async Task<ImapClient> GetConnectedClientAsync()
    {
        await _lock.WaitAsync();
        try
        {
            if (_client is { IsConnected: true, IsAuthenticated: true })
                return _client;

            _client?.Dispose();
            _client = new ImapClient();

            // TLS required — no plain-text fallback
            await _client.ConnectAsync(_host, _port, SecureSocketOptions.SslOnConnect);

            var password = _crypto.RetrievePassword(_username);
            await _client.AuthenticateAsync(_username, password);

            return _client;
        }
        finally
        {
            _lock.Release();
        }
    }

    private static EmailListItem ToListItem(IMessageSummary m) => new()
    {
        Id = m.UniqueId.ToString(),
        FromDisplay = m.Envelope.From.FirstOrDefault()?.ToString() ?? "(unknown)",
        Subject = m.Envelope.Subject ?? "(no subject)",
        DateDisplay = m.Date.LocalDateTime.ToString("g"),
        BodyText = "",
        BodyHtml = "",
        IsRead = m.Flags?.HasFlag(MessageFlags.Seen) ?? false
    };

    public void Dispose()
    {
        _client?.Disconnect(quit: true);
        _client?.Dispose();
        _lock.Dispose();
    }
}
