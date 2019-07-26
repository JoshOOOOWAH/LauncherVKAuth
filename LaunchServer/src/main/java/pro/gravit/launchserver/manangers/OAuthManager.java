package pro.gravit.launchserver.manangers;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.util.Timer;
import java.util.TimerTask;

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

        private long start_time;

        public String getIP(){
            return  IOHelper.getIP(this.getCtx().channel().remoteAddress());
        }

        public Entry(ChannelHandlerContext ctx, Client client,String code){
            this.code = code;
            this.ctx = ctx;
            this.client = client;
            this.isInit = true;
            this.start_time = System.currentTimeMillis();
        }

        public Entry(){
            this.code = null;
            this.ctx = null;
            this.client = null;
            this.isInit = false;
            this.start_time = 0L;
        }

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

        public boolean isOutdated() { return start_time > 300000 ? true : false; }
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
        if(search(getIP(ctx)) != null){
            deleteOther(ctx);
        }
        if(code == null)
        {
            LogHelper.error("code is null");
            return false;
        }
        for (int i = 0; i < server.cacheManager.stageArea.length; i++) {
            if(!server.cacheManager.stageArea[i].isInit()) {
                server.cacheManager.stageArea[i] = new Entry(ctx, client, code);
                LogHelper.subDebug("cache Stretched; " + code);
                int finalI = i;
                TimerTask tt = new TimerTask() {
                    @Override
                    public void run() {
                        if(server.cacheManager.stageArea[finalI].isOutdated()) {
                            LogHelper.subDebug("Outdated entry with code" + server.cacheManager.stageArea[finalI].getCode());
                            server.cacheManager.stageArea[finalI] = new Entry();
                        }
                    }
                };
                 new Timer().schedule(tt, 6000000);
                return true;
            }
        }
        return false;
    }

    public static void delete(ChannelHandlerContext ctx){
        for (int i = 0; i < server.cacheManager.stageArea.length; i++) {
            if(server.cacheManager.stageArea[i].isInit() && getIP(ctx).equals(server.cacheManager.stageArea[i].getIP())) {
                server.cacheManager.stageArea[i] = new Entry();
               LogHelper.subDebug("Delete after use");
            }
        }
    }

    public static Entry search(String ip){
        for (Entry e: server.cacheManager.stageArea) {
            if(e.isInit() && e.getIP().equals(ip)) {
                return e;
            }
        }
        return null;
    }

    public static void deleteOther(ChannelHandlerContext ctx){
        for (int i = 0; i < server.cacheManager.stageArea.length; i++) {
            Entry e = server.cacheManager.stageArea[i];
            if(e.isInit() && e.getIP().equals(getIP(ctx))) {
                LogHelper.subDebug("Deleting Entry with code " + e.getCode());
                server.cacheManager.stageArea[i] = new Entry();
                }
            }
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
