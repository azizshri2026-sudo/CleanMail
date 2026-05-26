using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;

namespace CleanMail.Windows.Services;

/// <summary>
/// Sends mail via MailKit SMTP. TLS mandatory (STARTTLS on port 587 or SSL on 465).
/// </summary>
public sealed class SmtpService : IDisposable
{
    private readonly CryptoService _crypto = App.Instance.CryptoService;

    private string _host = "";
    private int _port = 587;
    private string _username = "";
    private string _fromAddress = "";
    private string _fromName = "";

    public void Configure(string host, int port, string username, string fromAddress, string fromName = "")
    {
        _host = host;
        _port = port;
        _username = username;
        _fromAddress = fromAddress;
        _fromName = fromName;
    }

    public async Task SendAsync(string to, string cc, string subject, string bodyText, string bodyHtml = "")
    {
        var message = new MimeMessage();
        message.From.Add(new MailboxAddress(_fromName, _fromAddress));

        foreach (var addr in to.Split(',', ';').Select(s => s.Trim()).Where(s => s.Length > 0))
            message.To.Add(MailboxAddress.Parse(addr));

        if (!string.IsNullOrWhiteSpace(cc))
            foreach (var addr in cc.Split(',', ';').Select(s => s.Trim()).Where(s => s.Length > 0))
                message.Cc.Add(MailboxAddress.Parse(addr));

        message.Subject = subject;

        var builder = new BodyBuilder { TextBody = bodyText };
        if (!string.IsNullOrEmpty(bodyHtml)) builder.HtmlBody = bodyHtml;
        message.Body = builder.ToMessageBody();

        using var client = new SmtpClient();

        // STARTTLS required — reject plain-text connections
        await client.ConnectAsync(_host, _port, SecureSocketOptions.StartTls);

        var password = _crypto.RetrievePassword(_username);
        await client.AuthenticateAsync(_username, password);

        await client.SendAsync(message);
        await client.DisconnectAsync(quit: true);
    }

    public void Dispose() { }
}
