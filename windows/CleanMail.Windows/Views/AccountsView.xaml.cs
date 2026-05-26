using CleanMail.Windows.ViewModels;
using Microsoft.UI.Xaml.Controls;

namespace CleanMail.Windows.Views;

public sealed partial class AccountsView : Page
{
    public AccountsViewModel ViewModel { get; } = new();

    public AccountsView()
    {
        InitializeComponent();
    }

    private void AddAccount_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
        => Frame.Navigate(typeof(AccountSetupView));
}
