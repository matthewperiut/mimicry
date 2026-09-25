package com.slainlight.mimicry.client;

//? if <26.1 {
/*import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

// shows Bram's dialog files (data/mimicry/dialog) on versions that have no vanilla dialogs
public class BramDialogScreen extends Screen {
	private static final int LINE = 10;
	private static final int GAP = 8;

	private final JsonObject dialog;
	private final List<Body> body = new ArrayList<>();
	private int bodyTop;

	private record Body(ItemStack item, List<FormattedCharSequence> lines, int width) {
	}

	public BramDialogScreen(Identifier id) {
		this(load(id));
	}

	private BramDialogScreen(JsonObject dialog) {
		super(text(dialog.get("title")));
		this.dialog = dialog;
	}

	private static JsonObject load(Identifier id) {
		String path = "/data/" + id.getNamespace() + "/dialog/" + id.getPath() + ".json";
		try (InputStream in = Objects.requireNonNull(BramDialogScreen.class.getResourceAsStream(path), path);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Component text(JsonElement json) {
		//? if >=1.20.5 {
		return Component.Serializer.fromJson(json, net.minecraft.client.Minecraft.getInstance().level.registryAccess());
		//?} else {
		/^return Component.Serializer.fromJson(json);
		^///?}
	}

	@Override
	protected void init() {
		this.body.clear();
		int height = 0;
		for (JsonElement element : this.dialog.getAsJsonArray("body")) {
			JsonObject part = element.getAsJsonObject();
			ItemStack item = ItemStack.EMPTY;
			JsonObject message = part;
			if (part.has("item")) {
				JsonObject stack = part.getAsJsonObject("item");
				item = new ItemStack(BuiltInRegistries.ITEM.get(/^? if >=1.21 {^/Identifier.parse/^?} else {^//^new Identifier^//^?}^/(stack.get("id").getAsString())),
					stack.has("count") ? stack.get("count").getAsInt() : 1);
				message = part.getAsJsonObject("description");
			}
			int width = message.has("width") ? message.get("width").getAsInt() : 200;
			List<FormattedCharSequence> lines = this.font.split(text(message.get("contents")), width);
			this.body.add(new Body(item, lines, width));
			height += Math.max(item.isEmpty() ? 0 : 16, lines.size() * LINE) + GAP;
		}

		List<JsonObject> actions = new ArrayList<>();
		this.dialog.getAsJsonArray("actions").forEach(action -> actions.add(action.getAsJsonObject()));
		int columns = this.dialog.has("columns") ? this.dialog.get("columns").getAsInt() : 2;
		int rows = (actions.size() + columns - 1) / columns;
		int buttonsHeight = rows * 24 + (this.dialog.has("exit_action") ? 30 : 0);
		this.bodyTop = Math.max(30, (this.height - height - buttonsHeight) / 2);

		int y = this.bodyTop + height + GAP;
		for (int row = 0; row < rows; row++) {
			List<JsonObject> line = actions.subList(row * columns, Math.min(actions.size(), (row + 1) * columns));
			int total = line.stream().mapToInt(action -> width(action) + 4).sum() - 4;
			int x = (this.width - total) / 2;
			for (JsonObject action : line) {
				int w = width(action);
				this.addRenderableWidget(Button.builder(text(action.get("label")), button -> this.run(action.getAsJsonObject("action")))
					.bounds(x, y, w, 20).build());
				x += w + 4;
			}
			y += 24;
		}
		if (this.dialog.has("exit_action")) {
			JsonObject exit = this.dialog.getAsJsonObject("exit_action");
			int w = width(exit);
			this.addRenderableWidget(Button.builder(text(exit.get("label")), button -> this.onClose()).bounds((this.width - w) / 2, y + 6, w, 20).build());
		}
	}

	private static int width(JsonObject action) {
		return action.has("width") ? action.get("width").getAsInt() : 150;
	}

	private void run(JsonObject action) {
		String type = action.get("type").getAsString();
		if (type.equals("minecraft:show_dialog")) {
			this.minecraft.setScreen(new BramDialogScreen(/^? if >=1.21 {^/Identifier.parse/^?} else {^//^new Identifier^//^?}^/(action.get("dialog").getAsString())));
		} else if (type.equals("minecraft:run_command")) {
			this.onClose();
			this.minecraft.player.connection.sendCommand(action.get("command").getAsString());
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		//? if <1.20.2 {
		/^this.renderBackground(graphics);
		^///?}
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.bodyTop - 20, 0xFFFFFF);
		int y = this.bodyTop;
		for (Body part : this.body) {
			int textWidth = Math.min(part.width, part.lines.stream().mapToInt(this.font::width).max().orElse(0));
			int left = (this.width - textWidth - (part.item.isEmpty() ? 0 : 20)) / 2;
			if (!part.item.isEmpty()) {
				graphics.renderItem(part.item, left, y);
				graphics.renderItemDecorations(this.font, part.item, left, y);
				if (mouseX >= left && mouseX < left + 16 && mouseY >= y && mouseY < y + 16) {
					graphics.renderTooltip(this.font, part.item, mouseX, mouseY);
				}
				left += 20;
			}
			for (int i = 0; i < part.lines.size(); i++) {
				FormattedCharSequence line = part.lines.get(i);
				int x = part.item.isEmpty() ? (this.width - this.font.width(line)) / 2 : left;
				graphics.drawString(this.font, line, x, y + i * LINE, 0xFFFFFF);
			}
			y += Math.max(part.item.isEmpty() ? 0 : 16, part.lines.size() * LINE) + GAP;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return !this.dialog.has("pause") || this.dialog.get("pause").getAsBoolean();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !this.dialog.has("can_close_with_escape") || this.dialog.get("can_close_with_escape").getAsBoolean();
	}
}
*///?}
