using CleanMail.Windows.ViewModels;
using Microsoft.UI.Xaml.Controls;

namespace CleanMail.Windows.Views;

public sealed partial class ComposeView : Page
{
    public ComposeViewModel ViewModel { get; } = new(App.Instance.SmtpService);

    public ComposeView()
    {
        InitializeComponent();
    }

    private async void SendButton_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        bool sent = await ViewModel.SendAsync();
        if (sent) Frame.GoBack();
    }

    private void DiscardButton_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
        => Frame.GoBack();
}
