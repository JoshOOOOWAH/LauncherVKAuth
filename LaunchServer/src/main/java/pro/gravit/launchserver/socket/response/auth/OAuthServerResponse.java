package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launchserver.manangers.OAuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class OAuthServerResponse extends SimpleResponse {

    public String code;

    public OAuthServerResponse(String code){
        this.code = code;
    }

    @Override
    public String getType() {
        return "OAuthURL";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client clientData) {
        if(OAuthManager.stretch(ctx, clientData, this.code))
            sendError("Continue in Launcher");
        else
            sendError("Error");
    }
}
