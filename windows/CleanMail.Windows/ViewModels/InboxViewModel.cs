using CleanMail.Windows.Services;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace CleanMail.Windows.ViewModels;

public sealed class EmailListItem
{
    public required string Id { get; init; }
    public required string FromDisplay { get; init; }
    public required string Subject { get; init; }
    public required string DateDisplay { get; init; }
    public required string BodyText { get; init; }
    public required string BodyHtml { get; init; }
    public bool IsRead { get; init; }
}

public sealed class InboxViewModel : INotifyPropertyChanged
{
    private readonly ImapService _imap;
    private EmailListItem? _selectedEmail;
    private bool _isBusy;
    private string? _errorMessage;

    public ObservableCollection<EmailListItem> Emails { get; } = [];

    public EmailListItem? SelectedEmail
    {
        get => _selectedEmail;
        private set { _selectedEmail = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasSelection)); }
    }

    public bool HasSelection => _selectedEmail is not null;

    public bool IsBusy
    {
        get => _isBusy;
        private set { _isBusy = value; OnPropertyChanged(); }
    }

    public string? ErrorMessage
    {
        get => _errorMessage;
        private set { _errorMessage = value; OnPropertyChanged(); }
    }

    public InboxViewModel(ImapService imap)
    {
        _imap = imap;
    }

    public async Task LoadAsync()
    {
        IsBusy = true;
        ErrorMessage = null;
        try
        {
            var messages = await _imap.FetchInboxAsync();
            Emails.Clear();
            foreach (var m in messages) Emails.Add(m);
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
        }
        finally
        {
            IsBusy = false;
        }
    }

    public async Task SyncAsync()
    {
        IsBusy = true;
        ErrorMessage = null;
        try
        {
            var messages = await _imap.SyncInboxAsync();
            foreach (var m in messages)
            {
                if (!Emails.Any(e => e.Id == m.Id))
                    Emails.Insert(0, m);
            }
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
        }
        finally
        {
            IsBusy = false;
        }
    }

    public void SelectEmail(EmailListItem item)
    {
        SelectedEmail = item;
        if (!item.IsRead)
            _ = _imap.MarkReadAsync(item.Id);
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    private void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
