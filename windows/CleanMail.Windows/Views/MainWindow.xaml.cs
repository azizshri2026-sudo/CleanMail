using CleanMail.Windows.ViewModels;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;

namespace CleanMail.Windows.Views;

public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        ExtendsContentIntoTitleBar = true;
        NavView.SelectedItem = NavView.MenuItems[0];
        ContentFrame.Navigate(typeof(InboxView));
    }

    private void NavView_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (args.IsSettingsSelected)
        {
            ContentFrame.Navigate(typeof(SettingsView));
            return;
        }
        if (args.SelectedItem is NavigationViewItem item)
        {
            Type? page = item.Tag?.ToString() switch
            {
                "Inbox"    => typeof(InboxView),
                "Compose"  => typeof(ComposeView),
                "Accounts" => typeof(AccountsView),
                _          => null
            };
            if (page is not null) ContentFrame.Navigate(page);
        }
    }
}
