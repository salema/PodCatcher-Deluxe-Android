package net.alliknow.podcatcher.listeners;

/**
 * The callback definition, needs to implemented by the activity showing
 * this dialog.
 */
public interface OnEnterAuthorizationListener {
    /**
     * Called on the listener if the user submitted credentials.
     * 
     * @param username User name entered.
     * @param password Password entered.
     */
    public void onSubmitAuthorization(String username, String password);

    /**
     * Called on the listener if the user cancelled the dialog.
     */
    public void onCancelAuthorization();
}