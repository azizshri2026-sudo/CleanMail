using CleanMail.Windows.Services;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace CleanMail.Windows.ViewModels;

public sealed class ComposeViewModel : INotifyPropertyChanged
{
    private readonly SmtpService _smtp;
    private string _to = "";
    private string _cc = "";
    private string _subject = "";
    private string _body = "";
    private bool _isSending;
    private string? _errorMessage;

    public string To
    {
        get => _to;
        set { _to = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanSend)); }
    }

    public string Cc
    {
        get => _cc;
        set { _cc = value; OnPropertyChanged(); }
    }

    public string Subject
    {
        get => _subject;
        set { _subject = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanSend)); }
    }

    public string Body
    {
        get => _body;
        set { _body = value; OnPropertyChanged(); }
    }

    public bool IsSending
    {
        get => _isSending;
        private set { _isSending = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanSend)); }
    }

    public string? ErrorMessage
    {
        get => _errorMessage;
        private set { _errorMessage = value; OnPropertyChanged(); }
    }

    public bool CanSend => !IsSending && !string.IsNullOrWhiteSpace(To) && !string.IsNullOrWhiteSpace(Subject);

    public ComposeViewModel(SmtpService smtp)
    {
        _smtp = smtp;
    }

    public async Task<bool> SendAsync()
    {
        if (!CanSend) return false;
        IsSending = true;
        ErrorMessage = null;
        try
        {
            await _smtp.SendAsync(To, Cc, Subject, Body);
            return true;
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
            return false;
        }
        finally
        {
            IsSending = false;
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    private void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
