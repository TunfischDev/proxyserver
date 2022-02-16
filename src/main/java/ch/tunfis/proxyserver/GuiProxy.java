package ch.tunfis.proxyserver;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

public class GuiProxy extends Screen {
    private boolean isSocks4 = false;

    private TextFieldWidget ipPort;
    private TextFieldWidget username;
    private TextFieldWidget password;
    private CheckboxWidget enabledCheck;

    private Screen parentScreen;

    private String msg = "";

    private int[] positionY;
    private int positionX;

    private TestPing testPing = new TestPing();
    
    
	private static String text_proxy = new TranslatableText("ui.proxyserver.options.proxy").getString();


    public GuiProxy(Screen parentScreen) {
        super(new LiteralText(text_proxy));
        this.parentScreen = parentScreen;
    }

    private static boolean isValidIpPort(String ipP) {
        String[] split = ipP.split(":");
        if (split.length > 1) {
            if (!StringUtils.isNumeric(split[1])) return false;
            int port = Integer.parseInt(split[1]);
            if (port < 0 || port > 0xFFFF) return false;
            return true;
        } else {
            return false;
        }
    }

    private boolean checkProxy() {
        if (!isValidIpPort(ipPort.getText())) {
            msg = Formatting.RED + new TranslatableText("ui.proxyserver.options.invalidIpPort").getString();
            this.ipPort.setTextFieldFocused(true);
            return false;
        }
        return true;
    }

    private void centerButtons(int amount, int buttonLength, int gap) {
        positionX = (this.width / 2) - (buttonLength / 2);
        positionY = new int[amount];
        int center = (this.height + amount * gap) / 2;
        int buttonStarts = center - (amount * gap);
        for (int i = 0; i != amount; i++) {
            positionY[i] = buttonStarts + (gap * i);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        msg = "";
        testPing.state = "";
        return true;
    }

    // @OverrideopenScreen
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        if (enabledCheck.isChecked() && !isValidIpPort(ipPort.getText())) {
            enabledCheck.onPress();
        }

        drawStringWithShadow(matrixStack, this.textRenderer, new TranslatableText("ui.proxyserver.options.proxyType").getString(), this.width / 2 - 150, positionY[1] + 5, 10526880);
        drawCenteredText(matrixStack, this.textRenderer, new TranslatableText("ui.proxyserver.options.auth").getString(), this.width / 2, positionY[3] + 8, Formatting.WHITE.getColorValue());
        drawStringWithShadow(matrixStack, this.textRenderer, new TranslatableText("ui.proxyserver.options.ipPort").getString(), this.width / 2 - 150, positionY[2] + 5, 10526880);

        this.ipPort.render(matrixStack, mouseX, mouseY, partialTicks);
        if (isSocks4) {
            drawStringWithShadow(matrixStack, this.textRenderer, new TranslatableText("ui.proxyserver.auth.id").getString(), this.width / 2 - 150, positionY[4] + 5, 10526880);
            this.username.render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            drawStringWithShadow(matrixStack, this.textRenderer, new TranslatableText("ui.proxyserver.auth.username").getString(), this.width / 2 - 150, positionY[4] + 5, 10526880);
            drawStringWithShadow(matrixStack, this.textRenderer, new TranslatableText("ui.proxyserver.auth.password").getString(), this.width / 2 - 150, positionY[5] + 5, 10526880);
            this.username.render(matrixStack, mouseX, mouseY, partialTicks);
            this.password.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        drawCenteredText(matrixStack, this.textRenderer, !msg.isEmpty() ? msg : testPing.state, this.width / 2, positionY[6] + 5, 10526880);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick() {
        testPing.pingPendingNetworks();

        this.ipPort.tick();
        this.username.tick();
        this.password.tick();
    }

    @Override
    public void init() {
        MinecraftClient.getInstance().keyboard.setRepeatEvents(true);
        int buttonLength = 160;
        centerButtons(10, buttonLength, 26);

        isSocks4 = ProxyServer.proxy.type == Proxy.ProxyType.SOCKS4;

        ButtonWidget proxyType = new ButtonWidget(positionX, positionY[1], buttonLength, 20, new LiteralText(isSocks4 ? "Socks 4" : "Socks 5"), (button) -> {
            isSocks4 = !isSocks4;
            button.setMessage(new LiteralText(isSocks4 ? "Socks 4" : "Socks 5"));
        });
        this.addDrawableChild(proxyType);

        this.ipPort = new TextFieldWidget(this.textRenderer, positionX, positionY[2], buttonLength, 20, new LiteralText(""));
        this.ipPort.setText(ProxyServer.proxy.ipPort);
        this.ipPort.setMaxLength(1024);
        this.ipPort.setTextFieldFocused(true);
        this.addSelectableChild(this.ipPort);

        this.username = new TextFieldWidget(this.textRenderer, positionX, positionY[4], buttonLength, 20, new LiteralText(""));
        this.username.setMaxLength(255);
        this.username.setText(ProxyServer.proxy.username);
        this.addSelectableChild(this.username);

        this.password = new TextFieldWidget(this.textRenderer, positionX, positionY[5], buttonLength, 20, new LiteralText(""));
        this.password.setMaxLength(255);
        this.password.setText(ProxyServer.proxy.password);
        this.addSelectableChild(this.password);

        int posXButtons = (this.width / 2) - (((buttonLength / 2) * 3) / 2);

        ButtonWidget apply = new ButtonWidget(posXButtons, positionY[8], buttonLength / 2 - 3, 20,  new TranslatableText("ui.proxyserver.options.apply"), (button) -> {
            if (checkProxy()) {
                ProxyServer.proxy = new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText());
                ProxyServer.proxyEnabled = enabledCheck.isChecked();
                Config.setDefaultProxy(ProxyServer.proxy);
                Config.saveConfig();
                MinecraftClient.getInstance().setScreenAndRender(new MultiplayerScreen(new TitleScreen()));
            }
        });
        this.addDrawableChild(apply);

        ButtonWidget test = new ButtonWidget(posXButtons + buttonLength / 2 + 3, positionY[8], buttonLength / 2 - 3, 20, new TranslatableText("ui.proxyserver.options.test"), (button) -> {
            if (ipPort.getText().isEmpty() || ipPort.getText().equalsIgnoreCase("none")) {
                msg = Formatting.RED + new TranslatableText("ui.proxyserver.err.specProxy").getString();
                return;
            }
            if (checkProxy()) {
                testPing = new TestPing();
                testPing.run("mc.hypixel.net", 25565, new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText()));
            }
        });
        this.addDrawableChild(test);

        this.enabledCheck = new CheckboxWidget((this.width / 2) - (15 + textRenderer.getWidth(new TranslatableText("ui.proxyserver.options.proxyEnabled"))) / 2, positionY[7], buttonLength, 20, new TranslatableText("ui.proxyserver.options.proxyEnabled"), ProxyServer.proxyEnabled);
        this.addDrawableChild(this.enabledCheck);

        ButtonWidget cancel = new ButtonWidget(posXButtons + (buttonLength / 2 + 3) * 2, positionY[8], buttonLength / 2 - 3, 20, new TranslatableText("ui.proxyserver.options.cancel"), (button) -> {
            MinecraftClient.getInstance().setScreenAndRender(parentScreen);
        });
        this.addDrawableChild(cancel);
    }

    @Override
    public void onClose() {
        msg = "";
        MinecraftClient.getInstance().keyboard.setRepeatEvents(false);
    }
}