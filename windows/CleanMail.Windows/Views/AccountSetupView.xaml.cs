using CleanMail.Windows.ViewModels;
using Microsoft.UI.Xaml.Controls;

namespace CleanMail.Windows.Views;

public sealed partial class AccountSetupView : Page
{
    public AccountSetupViewModel ViewModel { get; } = new(App.Instance.CryptoService);

    public AccountSetupView()
    {
        InitializeComponent();
    }

    private async void SaveButton_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        bool saved = await ViewModel.SaveAsync();
        if (saved) Frame.GoBack();
    }
}
