using CleanMail.Windows.Services;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace CleanMail.Windows.ViewModels;

public sealed class AccountSetupViewModel : INotifyPropertyChanged
{
    private readonly CryptoService _crypto;

    private string _displayName = "";
    private string _email = "";
    private string _password = "";
    private string _imapHost = "";
    private string _imapPort = "993";
    private string _smtpHost = "";
    private string _smtpPort = "587";
    private bool _isSaving;
    private string? _errorMessage;

    public string DisplayName { get => _displayName; set { _displayName = value; OnPropertyChanged(); } }
    public string Email
    {
        get => _email;
        set
        {
            _email = value;
            OnPropertyChanged();
            AutoFillHosts(value);
            OnPropertyChanged(nameof(CanSave));
        }
    }
    public string Password   { get => _password;  set { _password  = value; OnPropertyChanged(); } }
    public string ImapHost   { get => _imapHost;  set { _imapHost  = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanSave)); } }
    public string ImapPort   { get => _imapPort;  set { _imapPort  = value; OnPropertyChanged(); } }
    public string SmtpHost   { get => _smtpHost;  set { _smtpHost  = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanSave)); } }
    public string SmtpPort   { get => _smtpPort;  set { _smtpPort  = value; OnPropertyChanged(); } }

    public bool IsSaving
    {
        get => _isSaving;
        private set { _isSaving = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanSave)); }
    }

    public string? ErrorMessage
    {
        get => _errorMessage;
        private set { _errorMessage = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasError)); }
    }

    public bool HasError => !string.IsNullOrEmpty(_errorMessage);
    public bool CanSave  => !IsSaving && !string.IsNullOrWhiteSpace(Email)
                            && !string.IsNullOrWhiteSpace(ImapHost)
                            && !string.IsNullOrWhiteSpace(SmtpHost);

    public AccountSetupViewModel(CryptoService crypto)
    {
        _crypto = crypto;
    }

    public async Task<bool> SaveAsync()
    {
        if (!CanSave) return false;
        IsSaving = true;
        ErrorMessage = null;
        try
        {
            // Store password in Windows Credential Manager
            if (!string.IsNullOrEmpty(Password))
                _crypto.StorePassword(Email, Password);

            // TODO: persist account metadata to SQLite local database
            await Task.Delay(50); // simulate DB write
            return true;
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
            return false;
        }
        finally
        {
            IsSaving = false;
        }
    }

    private void AutoFillHosts(string email)
    {
        var domain = email.Contains('@') ? email.Split('@').Last().ToLowerInvariant() : "";
        if (domain.Contains("gmail"))
        {
            ImapHost = "imap.gmail.com";
            SmtpHost = "smtp.gmail.com";
        }
        else if (domain.Contains("outlook") || domain.Contains("hotmail"))
        {
            ImapHost = "outlook.office365.com";
            SmtpHost = "smtp.office365.com";
        }
        else if (domain.Contains("yahoo"))
        {
            ImapHost = "imap.mail.yahoo.com";
            SmtpHost = "smtp.mail.yahoo.com";
        }
        else if (!string.IsNullOrEmpty(domain))
        {
            ImapHost = $"imap.{domain}";
            SmtpHost = $"smtp.{domain}";
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    private void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
