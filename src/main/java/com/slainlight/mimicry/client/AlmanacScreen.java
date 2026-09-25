package com.slainlight.mimicry.client;

import com.slainlight.mimicry.Hollowmere;
import com.slainlight.mimicry.MimicEntity;
import com.slainlight.mimicry.Mimicry;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.ChatFormatting;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
//? if <26.1 {
/*import net.minecraft.client.gui.screens.inventory.InventoryScreen;
*///?}
import net.minecraft.client.gui.screens.inventory.PageButton;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if >=26.1 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
//? if >=26.1 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}
//? if >=1.21.2 {
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.EntitySpawnReason;
//?}
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
//? if >=1.21.2 {
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
//?} else {
/*import net.minecraft.world.item.crafting.Ingredient;
//? if <1.20.5 {
/^import net.minecraft.world.item.crafting.RecipeManager;
^///?}
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
*///?}
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class AlmanacScreen extends Screen {
	private static final Identifier TEXTURE = Mimicry.id("textures/gui/almanac.png");
	// texture layout in almanac.png
	private static final int BOOK_W = 320, BOOK_H = 200, TEX_W = 512, TEX_H = 256;
	private static final int DIVIDER_U = 320, DIVIDER_V = 0, DIVIDER_W = 80, DIVIDER_H = 7;
	private static final int ARROW_U = 320, ARROW_V = 8, ARROW_W = 22, ARROW_H = 15;
	private static final int TAB_U = 320, TAB_V = 24, TAB_W = 14, TAB_H = 24, TAB_X = 272, TAB_SHOWS = 12;
	private static final int[] TEXT_X = {25, 173};
	private static final int TEXT_Y = 18, TEXT_W = 122, TEXT_H = 152, PAGE_NUMBER_Y = 175;
	private static final int LEADING = 10, PARAGRAPH_GAP = 4, ELEMENT_GAP = 6, CAP_LINES = 2;
	private static final int INK = 0x3B2A1E, RUBRIC = 0x8C2B14, FADED = 0x8A7560;

	private final List<List<Element>> pages = new ArrayList<>();
	private final List<Chapter> chapters = new ArrayList<>();
	private int spread;
	private int left;
	private int top;
	private PageButton back;
	private PageButton forward;
	private @Nullable MimicEntity mimic;
	private final @Nullable LecternMenu lectern;

	public AlmanacScreen() {
		this(null);
	}

	public AlmanacScreen(@Nullable LecternMenu lectern) {
		super(Component.translatable("item.mimicry.almanac"));
		//? if <26.1 {
		/*// the pages are laid out here, before init would set these
		this.minecraft = Minecraft.getInstance();
		this.font = this.minecraft.font;
		*///?}
		this.lectern = lectern;
		this.spread = lectern != null ? lectern.getPage() : 0;
		this.page(this.heading("story"), new Prose("story", true));
		this.page(this.heading("contents"), new Contents());
		this.chapter("chests", new Prose("chests.1", true));
		this.page(new Portrait(false), new Prose("chests.2", false), new Slots(this.slot(Mimicry.MIMIC_TOOTH, null)));
		this.chapter("lens", new Prose("lens.1", true), new RecipeView("treasure_lens"));
		this.page(new Prose("lens.2", false));
		this.chapter("taming", new Prose("taming.1", true), new Slots(this.slot(Items.GOLD_INGOT, "treat"), this.slot(Items.RAW_GOLD, "treat")));
		this.page(new Portrait(true), new Prose("taming.2", false));
		this.chapter("luggage", new Prose("luggage.1", true));
		this.page(new Prose("luggage.2", false), new Slots(this.slot(Items.GOLD_NUGGET, "mends.nugget"), this.slot(Items.GOLD_INGOT, "mends.ingot"),
			this.slot(Items.RAW_GOLD, "mends.ingot"), this.slot(Items.GOLD_BLOCK, "mends.block"), this.slot(Items.RAW_GOLD_BLOCK, "mends.block")));
		this.chapter("mimic_chest", new Prose("mimic_chest.1", true), new RecipeView("mimic_chest"));
		this.chapter("coffer", new Prose("coffer.1", true), new Slots(this.slot(Hollowmere.CROWN, null)));
		this.chapter("hollowmere", new Prose("hollowmere.1", true));
		this.page(new Prose("hollowmere.2", false));
		this.chapter("knights", new Prose("knights.1", true));
		this.page(new Prose("knights.2", false), new Slots(this.slot(Hollowmere.KNIGHT_SIGIL, null)));
		this.chapter("sunstone", new Prose("sunstone.1", true), new Slots(this.slot(Hollowmere.SUNSTONE_CLUSTER.asItem(), null), this.slot(Hollowmere.SUNSTONE_SHARD, null)));
		this.page(new RecipeView("sunstone_lantern"), new Prose("sunstone.2", false), new RecipeView("sunstone_chain"));
		this.chapter("chimneys", new Prose("chimneys.1", true), new RecipeView("brick_chimney", "stone_brick_chimney"));
		this.page(new Gap(26), new Portrait(false), new Gap(16), new Prose("end", false));
	}

	public List<String> overflows() {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < this.pages.size(); i++) {
			int height = -ELEMENT_GAP;
			for (Element element : this.pages.get(i)) {
				height += element.height() + ELEMENT_GAP;
			}
			if (height > TEXT_H) {
				out.add("page " + (i + 1) + " is " + (height - TEXT_H) + "px too long");
			}
		}
		return out;
	}

	public int spreads() {
		return (this.pages.size() + 1) / 2;
	}

	public void turnTo(int spread) {
		this.spread = Mth.clamp(spread, 0, this.spreads() - 1);
		this.back.visible = this.spread > 0;
		this.forward.visible = this.spread < this.spreads() - 1;
		if (this.lectern != null && this.lectern.getPage() != this.spread) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.lectern.containerId, LecternMenu.BUTTON_PAGE_JUMP_RANGE_START + this.spread);
		}
	}

	private void flip(int by) {
		int was = this.spread;
		this.turnTo(this.spread + by);
		if (this.spread != was) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
		}
	}

	@Override
	protected void init() {
		this.left = (this.width - BOOK_W) / 2;
		this.top = Math.max(TAB_SHOWS, (this.height - BOOK_H - 24 - TAB_SHOWS) / 2 + TAB_SHOWS);
		int buttonY = this.top + BOOK_H + 4;
		if (this.lectern != null && this.minecraft.player.mayBuild()) {
			this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, buttonY, 98, 20).build());
			this.addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"),
				button -> this.minecraft.gameMode.handleInventoryButtonClick(this.lectern.containerId, LecternMenu.BUTTON_TAKE_BOOK))
				.bounds(this.width / 2 + 2, buttonY, 98, 20).build());
		} else {
			this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, buttonY, 200, 20).build());
		}
		this.back = this.addRenderableWidget(new PageButton(this.left + 22, this.top + 172, false, button -> this.turnTo(this.spread - 1), true));
		this.forward = this.addRenderableWidget(new PageButton(this.left + BOOK_W - 45, this.top + 172, true, button -> this.turnTo(this.spread + 1), true));
		this.turnTo(this.spread);
	}

	//? if >=26.1 {
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
	//?} else if >=1.20.2 {
	/*@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		this.renderTransparentBackground(graphics); // what isInGameUi does on 26.x
	*///?} else {
	/*// called from render, 1.20.1 screens have no background pass
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		this.renderBackground(graphics);
	*///?}
		if (this.spread > 0) {
			int u = TAB_U + (this.overTab(mouseX, mouseY) ? TAB_W + 2 : 0);
			blit(graphics, this.left + TAB_X, this.top - TAB_SHOWS, u, TAB_V, TAB_W, TAB_H);
		}
		blit(graphics, this.left, this.top, 0, 0, BOOK_W, BOOK_H);
	}

	@Override
	//? if >=26.1 {
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
	//?} else {
	/*public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		//? if <1.20.2 {
		/^this.renderBackground(graphics, mouseX, mouseY, a);
		^///?}
		super.render(graphics, mouseX, mouseY, a);
	*///?}
		for (int side = 0; side < 2; side++) {
			int page = this.spread * 2 + side;
			if (page >= this.pages.size()) {
				continue;
			}
			int x = this.left + TEXT_X[side];
			int y = this.top + TEXT_Y;
			for (Element element : this.pages.get(page)) {
				element.draw(graphics, x, y, mouseX, mouseY);
				y += element.height() + ELEMENT_GAP;
			}
			Component number = Component.literal("- " + (page + 1) + " -");
			this.text(graphics, number, x + (TEXT_W - this.font.width(number)) / 2, this.top + PAGE_NUMBER_Y, FADED);
		}
		if (this.spread > 0 && this.overTab(mouseX, mouseY)) {
			//? if >=26.1 {
			graphics.setTooltipForNextFrame(this.font, Component.translatable("almanac.mimicry.contents.title"), mouseX, mouseY);
			//?} else {
			/*this.setTooltipForNextRenderPass(Component.translatable("almanac.mimicry.contents.title"));
			*///?}
		}
	}

	private boolean overTab(double mouseX, double mouseY) {
		return mouseX >= this.left + TAB_X && mouseX < this.left + TAB_X + TAB_W && mouseY >= this.top - TAB_SHOWS && mouseY < this.top;
	}

	//? if >=1.21.9 {
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.clickBook(event.x(), event.y())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == InputConstants.MOUSE_BUTTON_LEFT && this.clickBook(mouseX, mouseY)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
	*///?}

	private boolean clickBook(double mouseX, double mouseY) {
		if (this.spread > 0 && this.overTab(mouseX, mouseY)) {
			this.flip(-this.spread);
			return true;
		}
		for (int side = 0; side < 2; side++) {
			int page = this.spread * 2 + side;
			if (page >= this.pages.size()) {
				continue;
			}
			int y = this.top + TEXT_Y;
			for (Element element : this.pages.get(page)) {
				if (element.click(this.left + TEXT_X[side], y, mouseX, mouseY)) {
					return true;
				}
				y += element.height() + ELEMENT_GAP;
			}
		}
		return false;
	}

	@Override
	//? if >=1.20.2 {
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
	//?} else {
	/*public boolean mouseScrolled(double x, double y, double scrollY) {
	*///?}
		if (scrollY != 0) {
			this.flip(scrollY > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(x, y, /*? if >=1.20.2 {*/scrollX, /*?}*/scrollY);
	}

	//? if >=1.21.9 {
	@Override
	public boolean keyPressed(KeyEvent event) {
		return super.keyPressed(event) || this.turnKey(event.key());
	}
	//?} else {
	/*@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		return super.keyPressed(key, scanCode, modifiers) || this.turnKey(key);
	}
	*///?}

	private boolean turnKey(int key) {
		switch (key) {
			case InputConstants.KEY_LEFT, InputConstants.KEY_PAGEUP -> this.flip(-1);
			case InputConstants.KEY_RIGHT, InputConstants.KEY_PAGEDOWN -> this.flip(1);
			case InputConstants.KEY_HOME -> this.flip(-this.spread);
			default -> {
				return false;
			}
		}
		return true;
	}

	@Override
	public Component getNarrationMessage() {
		MutableComponent out = Component.empty().append(this.getTitle());
		for (int page = this.spread * 2; page < Math.min(this.spread * 2 + 2, this.pages.size()); page++) {
			for (Element element : this.pages.get(page)) {
				if (element instanceof Prose prose) {
					out.append(CommonComponents.NEW_LINE).append(prose.source.replace("*", ""));
				}
			}
		}
		return out;
	}

	//? if >=26.1 {
	@Override
	public boolean isInGameUi() {
		return true;
	}
	//?}

	@Override
	public boolean isPauseScreen() {
		return this.lectern == null;
	}

	@Override
	public void onClose() {
		if (this.lectern != null) {
			this.minecraft.player.closeContainer();
		}
		super.onClose();
	}

	private void page(Element... elements) {
		this.pages.add(List.of(elements));
	}

	private void chapter(String key, Element... elements) {
		this.chapters.add(new Chapter(Component.translatable("almanac.mimicry." + key + ".title").getString(), this.pages.size()));
		List<Element> page = new ArrayList<>();
		page.add(this.heading(key));
		Collections.addAll(page, elements);
		this.pages.add(page);
	}

	private Element heading(String key) {
		Component title = colored(Component.translatable("almanac.mimicry." + key + ".title").getString().toUpperCase(Locale.ROOT), RUBRIC);
		return new Element() {
			@Override
			public int height() {
				return 18;
			}

			@Override
			public void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY) {
				AlmanacScreen.this.text(graphics, title, x + (TEXT_W - AlmanacScreen.this.font.width(title)) / 2, y, RUBRIC);
				blit(graphics, x + (TEXT_W - DIVIDER_W) / 2, y + 11, DIVIDER_U, DIVIDER_V, DIVIDER_W, DIVIDER_H);
			}
		};
	}

	private Slot slot(Item item, @Nullable String note) {
		return new Slot(new ItemStack(item), note == null ? null : Component.translatable("almanac.mimicry." + note).withStyle(ChatFormatting.GOLD));
	}

	private MimicEntity mimic() {
		if (this.mimic == null) {
			//? if >=1.21.2 {
			this.mimic = Mimicry.MIMIC.create(this.minecraft.level, EntitySpawnReason.LOAD);
			//?} else {
			/*// the renderer reads the entity, which never ticks here, so the portrait's mouth comes from the clock
			this.mimic = new MimicEntity(Mimicry.MIMIC, this.minecraft.level) {
				@Override
				public float getMouthOpen(float partialTicks) {
					return AlmanacScreen.mouth(this.isTame(), Util.getMillis() / 50.0F);
				}
			};
			*///?}
			if (this.mimic != null) {
				this.mimic.setId(-1); // never added to the level, but rendering needs an id
			}
		}
		return this.mimic;
	}

	private static float mouth(boolean tame, float ticks) {
		float snap = ticks % 24.0F / 10.0F;
		return tame ? 0.3F + 0.05F * Mth.sin(ticks * 0.4F) : snap < 1.0F ? Mth.sin(snap * Mth.PI) * 0.9F : 0.06F;
	}

	private static void blit(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int u, int v, int width, int height) {
		graphics.blit(/*? if >=26.1 {*/RenderPipelines.GUI_TEXTURED, /*?}*/TEXTURE, x, y, u, v, width, height, TEX_W, TEX_H);
	}

	private static MutableComponent colored(String text, int color) {
		return Component.literal(text)./*? if >=1.20.3 {*/withColor(color)/*?} else {*//*withStyle(style -> style.withColor(color))*//*?}*/;
	}

	private void text(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, Component text, int x, int y, int color) {
		graphics./*? if >=26.1 {*/text/*?} else {*//*drawString*//*?}*/(this.font, text, x, y, 0xFF000000 | color, false);
	}

	private void slot(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int size, ItemStack stack, @Nullable Component note, int mouseX, int mouseY) {
		graphics.fill(x, y, x + size, y + size, 0xFF8B8B8B);
		graphics.fill(x, y, x + size - 1, y + 1, 0xFF373737);
		graphics.fill(x, y, x + 1, y + size - 1, 0xFF373737);
		graphics.fill(x + 1, y + size - 1, x + size, y + size, 0xFFFFFFFF);
		graphics.fill(x + size - 1, y + 1, x + size, y + size, 0xFFFFFFFF);
		int ix = x + (size - 16) / 2;
		int iy = y + (size - 16) / 2;
		if (stack.isEmpty()) {
			return;
		}
		//? if >=26.1 {
		graphics.item(stack, ix, iy);
		graphics.itemDecorations(this.font, stack, ix, iy);
		//?} else {
		/*graphics.renderItem(stack, ix, iy);
		graphics.renderItemDecorations(this.font, stack, ix, iy);
		*///?}
		if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
			graphics.fill(/*? if <26.1 {*//*RenderType.guiOverlay(), *//*?}*/ix, iy, ix + 16, iy + 16, 0x80FFFFFF);
			List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, stack));
			if (note != null) {
				lines.add(note);
			}
			//? if >=26.1 {
			graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
			//?} else {
			/*this.setTooltipForNextRenderPass(lines.stream().map(Component::getVisualOrderText).toList());
			*///?}
		}
	}

	private record Chapter(String title, int page) {
	}

	private record Slot(ItemStack stack, @Nullable Component note) {
	}

	private record Gap(int height) implements Element {
		@Override
		public void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY) {
		}
	}

	private interface Element {
		int height();

		void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY);

		default boolean click(int x, int y, double mouseX, double mouseY) {
			return false;
		}
	}

	// lang markup: text between asterisks is set in red; a paragraph starting "- " is right-aligned
	private final class Prose implements Element {
		private record Word(Component text, int width) {
		}

		private record Line(List<Word> words, int x, int y, int width, boolean last, boolean right) {
		}

		private final String source;
		private final List<Line> lines = new ArrayList<>();
		private @Nullable Component capital;
		private final int height;

		Prose(String key, boolean dropCap) {
			String text = Component.translatable("almanac.mimicry." + key).getString();
			this.source = text;
			int capWidth = 0;
			if (dropCap) {
				int first = text.codePointAt(0);
				this.capital = colored(Character.toString(first), RUBRIC);
				text = text.substring(Character.charCount(first));
				capWidth = AlmanacScreen.this.font.width(this.capital) * 2 + 1;
			}
			int space = AlmanacScreen.this.font.width(" ");
			int y = 0;
			boolean rubric = false;
			for (String paragraph : text.split("\n")) {
				boolean signature = paragraph.startsWith("- ");
				List<Word> line = new ArrayList<>();
				int used = 0;
				for (String raw : paragraph.split(" ")) {
					if (raw.isEmpty()) {
						continue;
					}
					MutableComponent word = Component.empty();
					StringBuilder run = new StringBuilder();
					for (char c : raw.toCharArray()) {
						if (c == '*') {
							word.append(colored(run.toString(), rubric ? RUBRIC : INK));
							run.setLength(0);
							rubric = !rubric;
						} else {
							run.append(c);
						}
					}
					word.append(colored(run.toString(), rubric ? RUBRIC : INK));
					int width = AlmanacScreen.this.font.width(word);
					int indent = this.lines.size() < CAP_LINES ? capWidth : 0;
					if (!line.isEmpty() && used + space + width > TEXT_W - indent) {
						this.lines.add(new Line(line, indent, y, TEXT_W - indent, false, signature));
						y += LEADING;
						line = new ArrayList<>();
						used = 0;
					}
					used += (line.isEmpty() ? 0 : space) + width;
					line.add(new Word(word, width));
				}
				int indent = this.lines.size() < CAP_LINES ? capWidth : 0;
				this.lines.add(new Line(line, indent, y, TEXT_W - indent, true, signature));
				y += LEADING + PARAGRAPH_GAP;
			}
			this.height = Math.max(y - LEADING - PARAGRAPH_GAP + AlmanacScreen.this.font.lineHeight, this.capital != null ? 16 : 0);
		}

		@Override
		public int height() {
			return this.height;
		}

		@Override
		public void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY) {
			if (this.capital != null) {
				//? if >=26.1 {
				graphics.pose().pushMatrix();
				graphics.pose().translate(x, y);
				graphics.pose().scale(2.0F, 2.0F);
				//?} else {
				/*graphics.pose().pushPose();
				graphics.pose().translate(x, y, 0.0F);
				graphics.pose().scale(2.0F, 2.0F, 1.0F);
				*///?}
				AlmanacScreen.this.text(graphics, this.capital, 0, 0, RUBRIC);
				graphics.pose()./*? if >=26.1 {*/popMatrix/*?} else {*//*popPose*//*?}*/();
			}
			int space = AlmanacScreen.this.font.width(" ");
			for (Line line : this.lines) {
				int natural = -space;
				for (Word word : line.words) {
					natural += word.width + space;
				}
				int gaps = line.words.size() - 1;
				int extra = line.width - natural;
				boolean justify = !line.last && !line.right && gaps > 0 && extra <= gaps * 2;
				int wx = x + line.x + (line.right ? extra : 0);
				for (int i = 0; i < line.words.size(); i++) {
					Word word = line.words.get(i);
					AlmanacScreen.this.text(graphics, word.text, wx, y + line.y, INK);
					wx += word.width + space + (justify ? extra / gaps + (i < extra % gaps ? 1 : 0) : 0);
				}
			}
		}
	}

	private final class Contents implements Element {
		private static final int ROW = 11;

		@Override
		public int height() {
			return AlmanacScreen.this.chapters.size() * ROW;
		}

		@Override
		public void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY) {
			for (int i = 0; i < AlmanacScreen.this.chapters.size(); i++) {
				Chapter chapter = AlmanacScreen.this.chapters.get(i);
				int row = y + i * ROW;
				boolean hovered = mouseX >= x && mouseX < x + TEXT_W && mouseY >= row - 1 && mouseY < row + ROW - 1;
				MutableComponent title = colored(chapter.title, hovered ? RUBRIC : INK);
				Component number = Component.literal(String.valueOf(chapter.page + 1));
				int titleWidth = AlmanacScreen.this.font.width(title);
				int numberX = x + TEXT_W - AlmanacScreen.this.font.width(number);
				AlmanacScreen.this.text(graphics, title, x, row, INK);
				AlmanacScreen.this.text(graphics, number, numberX, row, hovered ? RUBRIC : INK);
				for (int dot = x + titleWidth + 3; dot < numberX - 2; dot += 3) {
					graphics.fill(dot, row + 7, dot + 1, row + 8, 0xFF000000 | FADED);
				}
				if (hovered) {
					graphics.fill(x, row + 9, x + titleWidth - 1, row + 10, 0xFF000000 | RUBRIC);
				}
			}
		}

		@Override
		public boolean click(int x, int y, double mouseX, double mouseY) {
			int i = Mth.floor((mouseY - y + 1) / ROW);
			if (mouseX < x || mouseX >= x + TEXT_W || mouseY < y - 1 || i < 0 || i >= AlmanacScreen.this.chapters.size()) {
				return false;
			}
			AlmanacScreen.this.flip(AlmanacScreen.this.chapters.get(i).page / 2 - AlmanacScreen.this.spread);
			return true;
		}
	}

	private final class Slots implements Element {
		private final Slot[] slots;

		Slots(Slot... slots) {
			this.slots = slots;
		}

		@Override
		public int height() {
			return 18;
		}

		@Override
		public void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY) {
			int sx = x + (TEXT_W - this.slots.length * 18) / 2;
			for (int i = 0; i < this.slots.length; i++) {
				AlmanacScreen.this.slot(graphics, sx + i * 18, y, 18, this.slots[i].stack, this.slots[i].note, mouseX, mouseY);
			}
		}
	}

	// the client isn't sent recipes, so they're read from the mod's own data files
	private final class RecipeView implements Element {
		private record Grid(int width, int height, List<List<ItemStack>> slots, ItemStack result) {
		}

		private final List<Grid> grids = new ArrayList<>();
		private final int gridWidth;
		private final int gridHeight;

		RecipeView(String... names) {
			//? if >=1.21.2 {
			ContextMap context = SlotDisplayContext.fromLevel(AlmanacScreen.this.minecraft.level);
			for (String name : names) {
				RecipeDisplay display = load(name).display().getFirst();
				List<SlotDisplay> ingredients;
				int width;
				if (display instanceof ShapedCraftingRecipeDisplay shaped) {
					ingredients = shaped.ingredients();
					width = shaped.width();
				} else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
					ingredients = shapeless.ingredients();
					width = ingredients.size() <= 1 ? 1 : ingredients.size() <= 4 ? 2 : 3;
				} else {
					throw new IllegalArgumentException("the almanac can't draw recipe " + name);
				}
				this.grids.add(new Grid(width, Mth.positiveCeilDiv(ingredients.size(), width),
					ingredients.stream().map(slot -> slot.resolveForStacks(context)).toList(), display.result().resolveForFirstStack(context)));
			}
			//?} else {
			/*for (String name : names) {
				Recipe<?> recipe = load(name);
				List<Ingredient> ingredients = recipe.getIngredients();
				int width;
				if (recipe instanceof ShapedRecipe shaped) {
					width = shaped.getWidth();
				} else if (recipe instanceof ShapelessRecipe) {
					width = ingredients.size() <= 1 ? 1 : ingredients.size() <= 4 ? 2 : 3;
				} else {
					throw new IllegalArgumentException("the almanac can't draw recipe " + name);
				}
				this.grids.add(new Grid(width, Mth.positiveCeilDiv(ingredients.size(), width),
					ingredients.stream().map(ingredient -> List.of(ingredient.getItems())).toList(), recipe.getResultItem(AlmanacScreen.this.minecraft.level.registryAccess())));
			}
			*///?}
			this.gridWidth = this.grids.stream().mapToInt(Grid::width).max().orElse(0);
			this.gridHeight = this.grids.stream().mapToInt(Grid::height).max().orElse(0);
		}

		private Recipe<?> load(String name) {
			String path = "data/" + Mimicry.MOD_ID + /*? if >=1.21 {*/"/recipe/"/*?} else {*//*"/recipes/"*//*?}*/ + name + ".json";
			try (Reader reader = new InputStreamReader(Objects.requireNonNull(AlmanacScreen.class.getResourceAsStream("/" + path), path), StandardCharsets.UTF_8)) {
				//? if >=1.20.5 {
				return Recipe./*? if >=26.3 {*/DIRECT_CODEC/*?} else {*//*CODEC*//*?}*/.parse(RegistryOps.create(JsonOps.INSTANCE, AlmanacScreen.this.minecraft.level.registryAccess()),
					JsonParser.parseReader(reader)).getOrThrow();
				//?} else {
				/*return RecipeManager.fromJson(Mimicry.id(name), JsonParser.parseReader(reader).getAsJsonObject());
				*///?}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public int height() {
			return Math.max(this.gridHeight * 18, 26);
		}

		@Override
		public void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY) {
			long second = Util.getMillis() / 1000;
			Grid grid = this.grids.get((int) (second / 2 % this.grids.size()));
			int gx = x + (TEXT_W - (this.gridWidth * 18 + 6 + ARROW_W + 6 + 26)) / 2;
			int gy = y + (this.height() - grid.height * 18) / 2;
			for (int i = 0; i < grid.slots.size(); i++) {
				List<ItemStack> options = grid.slots.get(i);
				ItemStack stack = options.isEmpty() ? ItemStack.EMPTY : options.get((int) (second % options.size()));
				AlmanacScreen.this.slot(graphics, gx + i % grid.width * 18, gy + i / grid.width * 18, 18, stack, null, mouseX, mouseY);
			}
			int ax = gx + this.gridWidth * 18 + 6;
			blit(graphics, ax, y + (this.height() - ARROW_H) / 2, ARROW_U, ARROW_V, ARROW_W, ARROW_H);
			AlmanacScreen.this.slot(graphics, ax + ARROW_W + 6, y + (this.height() - 26) / 2, 26, grid.result, null, mouseX, mouseY);
		}
	}

	private final class Portrait implements Element {
		private final boolean tame;

		Portrait(boolean tame) {
			this.tame = tame;
		}

		@Override
		public int height() {
			return 36;
		}

		@Override
		public void draw(/*? if >=26.1 {*/GuiGraphicsExtractor/*?} else {*//*GuiGraphics*//*?}*/ graphics, int x, int y, int mouseX, int mouseY) {
			MimicEntity mimic = AlmanacScreen.this.mimic();
			if (mimic == null) {
				return;
			}
			int cx = x + TEXT_W / 2;
			int cy = y + this.height() / 2;
			float yaw = (float) Math.atan((cx - mouseX) / 40.0F);
			float pitch = (float) Math.atan((cy - mouseY) / 40.0F);
			Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
			Quaternionf tilt = new Quaternionf().rotateX(pitch * 20.0F * Mth.DEG_TO_RAD);
			rotation.mul(tilt);
			//? if >=26.1 {
			MimicRenderer.State state = (MimicRenderer.State) AlmanacScreen.this.minecraft.getEntityRenderDispatcher().getRenderer(mimic).createRenderState(mimic, 1.0F);
			state.shadowPieces.clear();
			state.outlineColor = 0;
			state.bodyRot = 180.0F + yaw * 20.0F;
			state.yRot = yaw * 20.0F;
			state.xRot = -pitch * 20.0F;
			float ticks = Util.getMillis() / 50.0F; // the game may be paused, so use wall-clock time
			state.ageInTicks = ticks;
			state.tame = this.tame;
			state.health = 1.0F;
			state.mouth = mouth(this.tame, ticks);
			// the entity is centred in, and clipped to, this box; it's oversized so the open lid and jaw aren't cut off
			graphics.entity(state, 36.0F, new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F), rotation, tilt, cx - 48, cy - 40, cx + 48, cy + 40);
			//?} else {
			/*mimic.yBodyRot = 180.0F + yaw * 20.0F;
			mimic.setYRot(180.0F + yaw * 40.0F);
			mimic.yHeadRot = mimic.getYRot();
			mimic.setXRot(-pitch * 20.0F);
			mimic.tickCount = (int) (Util.getMillis() / 50);
			mimic.setTame(this.tame/^? if >=1.20.5 {^/, false/^?}^/);
			graphics.enableScissor(cx - 48, cy - 40, cx + 48, cy + 40);
			//? if >=1.20.5 {
			InventoryScreen.renderEntityInInventory(graphics, cx, cy, 36.0F, new Vector3f(0.0F, mimic.getBbHeight() / 2.0F + 0.0625F, 0.0F), rotation, tilt, mimic);
			//?} else {
			/^InventoryScreen.renderEntityInInventory(graphics, cx, cy + Math.round((mimic.getBbHeight() / 2.0F + 0.0625F) * 36.0F), 36, rotation, tilt, mimic);
			^///?}
			graphics.disableScissor();
			*///?}
		}
	}
}
