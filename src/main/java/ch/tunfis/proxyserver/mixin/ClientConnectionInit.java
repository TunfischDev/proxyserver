package ch.tunfis.proxyserver.mixin;

import ch.tunfis.proxyserver.ProxyServer;
import io.netty.channel.Channel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ch.tunfis.proxyserver.Proxy;

import java.net.InetSocketAddress;

@Mixin(targets = "net/minecraft/network/ClientConnection$1")
public class ClientConnectionInit {
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("HEAD"))
    private void connect(Channel channel, CallbackInfo cir) {
        Proxy proxy = ProxyServer.proxy;

        if (ProxyServer.proxyEnabled) {
            ProxyServer.lastUsedProxy = proxy;

            if (proxy.type == Proxy.ProxyType.SOCKS5) {
                channel.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username.isEmpty() ? null : proxy.username, proxy.password.isEmpty() ? null : proxy.password));
            } else {
                channel.pipeline().addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username.isEmpty() ? null : proxy.username));
            }
        } else {
            ProxyServer.lastUsedProxy = new Proxy();
        }

        ProxyServer.proxyMenuButton.setMessage(new LiteralText("Proxy Settings"));
    }
}