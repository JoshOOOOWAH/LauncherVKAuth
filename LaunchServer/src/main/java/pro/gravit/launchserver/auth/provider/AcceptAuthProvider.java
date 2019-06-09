package pro.gravit.launchserver.auth.provider;

import pro.gravit.utils.helper.SecurityHelper;

public final class AcceptAuthProvider extends AuthProvider {

    @Override
    public AuthProviderResult auth(String login, String password, String ip) {
        return new AuthProviderResult(login, SecurityHelper.randomStringToken(), srv); // Same as login
    }

    @Override
    public AuthProviderResult oauth(String id) throws Exception {
        return new AuthProviderResult(id, SecurityHelper.randomStringToken(), srv);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
