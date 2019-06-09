package pro.gravit.launchserver.manangers;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class OAuthManager implements NeedGarbageCollection {

    @Override
    public void garbageCollection() {
        server.cacheManager.stageArea = new Entry[]{
                new Entry(), new Entry(), new Entry()
        };
    }

    public static class Entry{

        private boolean isInit;

        private ChannelHandlerContext ctx;
        private Client client;
        public String code;

        public String getIP(){
            return  IOHelper.getIP(this.getCtx().channel().remoteAddress());
        }

        public Entry(ChannelHandlerContext ctx, Client client,String code){
            this.code = code;
            this.ctx = ctx;
            this.client = client;
            this.isInit = true;
        }

        public Entry(){}

        public ChannelHandlerContext getCtx() {
            return ctx;
        }

        public Client getClient() {
            return client;
        }

        public String getCode() {
            return code;
        }

        public boolean isInit() {
            return isInit;
        }
    }

    public Entry[] stageArea;

    public static LaunchServer server;
    public OAuthManager(LaunchServer server){
        this.server = server;
        stageArea = new Entry[]{
                new Entry(), new Entry(), new Entry()
        };
    }

    public static boolean stretch(ChannelHandlerContext ctx, Client client, String code){
        if(code == null)
        {
            LogHelper.error("code is null");
            return false;
        }
        for (int i = 0; i < server.cacheManager.stageArea.length; i++) {
            if(!server.cacheManager.stageArea[i].isInit()) {
                server.cacheManager.stageArea[i] = new Entry(ctx, client, code);
                LogHelper.subDebug("cache Stretched; " + code);
                return true;
            }
        }
        return false;
    }

    public static Entry search(String ip){
        for (Entry e: server.cacheManager.stageArea) {
            if(e.isInit() && e.getIP().equals(ip)) {
                return e;
            }
        }
        return null;
    }

    public static String getIP(ChannelHandlerContext ctx){
        return IOHelper.getIP(ctx.channel().remoteAddress());
    }

    public static String getID(String code){
        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);
        try {
            UserAuthResponse authResponse = vk.oAuth()
                    .userAuthorizationCodeFlow(
                            server.config.OAuth.ID,
                            server.config.OAuth.Secret,
                            server.config.OAuth.BackURL, code)
                    .execute();
            return String.valueOf(authResponse.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
