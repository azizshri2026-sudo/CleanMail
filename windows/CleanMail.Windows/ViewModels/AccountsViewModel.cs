using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace CleanMail.Windows.ViewModels;

public sealed class AccountEntry
{
    public required string Id { get; init; }
    public required string DisplayName { get; init; }
    public required string EmailAddress { get; init; }
}

public sealed class AccountsViewModel : INotifyPropertyChanged
{
    public ObservableCollection<AccountEntry> Accounts { get; } = [];

    public AccountsViewModel()
    {
        LoadAccounts();
    }

    private void LoadAccounts()
    {
        // TODO: load from SQLite local database
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    private void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
