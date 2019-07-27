package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.OAuthRequestEvent;
import pro.gravit.launcher.hwid.OshiHWID;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.hwid.HWIDException;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.manangers.OAuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.VerifyHelper;


import java.security.SecureRandom;
import java.util.*;

public class OAuthResponse extends SimpleResponse {
    public transient static Random random = new SecureRandom();

    public OAuthResponse(OshiHWID hwid) {
        this.hwid = hwid;
    }

    public OshiHWID hwid;

    @Override
    public String getType() {
        return "oAuth";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client clientData) throws Exception {
        try {
            OAuthRequestEvent result = new OAuthRequestEvent();
            if (clientData == null || !clientData.checkSign) {
                AuthProvider.authError("Don't skip Launcher Update");
                return;
            }
            long startTime = System.currentTimeMillis();
            if(OAuthManager.search(OAuthManager.getIP(ctx)) == null){
                while(OAuthManager.search(OAuthManager.getIP(ctx)) == null){
                    Thread.sleep(5000);
                    if((System.currentTimeMillis() - startTime  > 3000000)) {
                        sendError("TimedOut");
                        return;
                    }
                }
            }

            String id = OAuthManager.getID(OAuthManager.search(OAuthManager.getIP(ctx)).getCode());
            if(id == null)
                throw new AuthException("Invalid code");

            OAuthManager.delete(ctx);
            OAuthManager.deleteOther(ctx);

            AuthProviderPair pair;
            pair = server.config.getAuthProviderPair("MySQLProvider");
            if(pair == null)
                throw new AuthException("Not defined MySQLProvider");
            AuthProvider provider = pair.provider;
            TextureProvider textureProvider =  pair.textureProvider;
            AuthProviderResult aresult = provider.oauth(id);
            if (!VerifyHelper.isValidUsername(aresult.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", aresult.username));
                return;
            }

            Collection<ClientProfile> profiles = server.getProfiles();
            for (ClientProfile p : profiles) {
                    if (!p.isWhitelistContains(aresult.username)) {
                        throw new AuthException(server.config.whitelistRejectString);
                    }
                    clientData.profile = p;
            }

            server.config.hwidHandler.check(hwid, aresult.username);
            clientData.isAuth = true;
            clientData.permissions = aresult.permissions;
            clientData.auth_id = "OAuth";
            clientData.updateAuth(server);
            result.accessToken = aresult.accessToken;
            result.permissions = clientData.permissions;
            if (clientData.session == 0) {
                    clientData.session = random.nextLong();
                    server.sessionManager.addClient(clientData);
                }

            result.session = clientData.session;
                UUID uuid = pair.handler.auth(aresult);

                /*if (uuid == null)
                    LogHelper.subWarning("uuid is null");
                if (aresult.username == null)
                    LogHelper.subWarning("aresult.username is null");
                if (clientData.auth.textureProvider == null)
                    LogHelper.subWarning("clientData.auth.textureProvider is null");
                if (result.playerProfile == null)
                    LogHelper.subWarning("result.playerProfile is null");*/

                result.playerProfile = ProfileByUUIDResponse.getProfile( uuid, aresult.username, null, textureProvider);;
                LogHelper.debug("Auth: %s accessToken %s uuid: %s", aresult.username, result.accessToken, uuid.toString());

            sendResult(result);
        } catch (AuthException | HWIDException | HookException e) {
            sendError(e.getMessage());
        }
    }

}
