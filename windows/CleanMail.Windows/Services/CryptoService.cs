using Windows.Security.Credentials;

namespace CleanMail.Windows.Services;

/// <summary>
/// Stores and retrieves secrets via Windows Credential Manager (PasswordVault).
/// Secrets never touch disk in plaintext — OS encrypts them with DPAPI.
/// </summary>
public sealed class CryptoService
{
    private const string VaultResource = "CleanMail";
    private const string OAuthResource = "CleanMail.OAuth";

    private readonly PasswordVault _vault = new();

    // --- Password credentials (IMAP/SMTP) ---

    public void StorePassword(string username, string password)
    {
        // Remove any existing entry first (PasswordVault throws on duplicates)
        TryDeletePassword(username);
        _vault.Add(new PasswordCredential(VaultResource, username, password));
    }

    public string RetrievePassword(string username)
    {
        var cred = _vault.Retrieve(VaultResource, username);
        cred.RetrievePassword();
        return cred.Password;
    }

    public bool TryDeletePassword(string username)
    {
        try
        {
            var cred = _vault.Retrieve(VaultResource, username);
            _vault.Remove(cred);
            return true;
        }
        catch
        {
            return false;
        }
    }

    // --- OAuth tokens ---

    private const string TokenSuffix_Access  = ":access";
    private const string TokenSuffix_Refresh = ":refresh";

    public void StoreOAuthTokens(string accountId, string accessToken, string refreshToken)
    {
        TryDeleteOAuthTokens(accountId);
        _vault.Add(new PasswordCredential(OAuthResource, accountId + TokenSuffix_Access,  accessToken));
        _vault.Add(new PasswordCredential(OAuthResource, accountId + TokenSuffix_Refresh, refreshToken));
    }

    public (string Access, string Refresh) RetrieveOAuthTokens(string accountId)
    {
        var access  = _vault.Retrieve(OAuthResource, accountId + TokenSuffix_Access);
        var refresh = _vault.Retrieve(OAuthResource, accountId + TokenSuffix_Refresh);
        access.RetrievePassword();
        refresh.RetrievePassword();
        return (access.Password, refresh.Password);
    }

    public bool TryDeleteOAuthTokens(string accountId)
    {
        bool deleted = false;
        foreach (var suffix in new[] { TokenSuffix_Access, TokenSuffix_Refresh })
        {
            try
            {
                var cred = _vault.Retrieve(OAuthResource, accountId + suffix);
                _vault.Remove(cred);
                deleted = true;
            }
            catch { /* not stored — ignore */ }
        }
        return deleted;
    }

    public bool HasOAuthTokens(string accountId)
    {
        try
        {
            _vault.Retrieve(OAuthResource, accountId + TokenSuffix_Access);
            return true;
        }
        catch
        {
            return false;
        }
    }
}
