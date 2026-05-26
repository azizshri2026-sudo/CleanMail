using CleanMail.Windows.ViewModels;
using Microsoft.UI.Xaml.Controls;

namespace CleanMail.Windows.Views;

public sealed partial class InboxView : Page
{
    public InboxViewModel ViewModel { get; } = new(App.Instance.ImapService);

    public InboxView()
    {
        InitializeComponent();
        _ = ViewModel.LoadAsync();
    }

    private void EmailList_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (EmailList.SelectedItem is EmailListItem item)
            ViewModel.SelectEmail(item);
    }

    private async void SyncButton_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
        => await ViewModel.SyncAsync();

    private void ComposeButton_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
        => Frame.Navigate(typeof(ComposeView));
}
