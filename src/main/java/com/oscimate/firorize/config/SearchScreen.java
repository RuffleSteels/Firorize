package com.oscimate.firorize.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.List;

public class SearchScreen extends Screen {
    private Screen parent;
    private SearchScreen.SearchScreenListWidget languageSelectionList;

    private final List<Block> blockUnderList = Registries.BLOCK.stream().filter(block -> Block.isFaceFullSquare(block.getDefaultState().getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, ShapeContext.absent()), Direction.UP)).toList();
    final LanguageManager languageManager;

    protected SearchScreen(Screen parent, LanguageManager languageManager) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
        this.languageManager = languageManager;
    }
    private TextFieldWidget textFieldWidget;
    private String input = "";
    @Override
    protected void init() {
        this.languageSelectionList = new SearchScreen.SearchScreenListWidget(this.client);
        this.addDrawableChild(languageSelectionList);
        textFieldWidget = new TextFieldWidget(this.textRenderer, this.width / 2, 5, 150, 20, ScreenTexts.DONE);
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.onDone()).dimensions(this.width / 2 - 155 + 160, this.height - 38, 150, 20).build());
        this.addDrawableChild(textFieldWidget);
        textFieldWidget.setChangedListener(this::onChanged);

    }

    private void onChanged(String input) {
        this.input=input;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.languageSelectionList.test();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    void onDone() {
        this.client.setScreen(this.parent);
    }


    @Environment(value= EnvType.CLIENT)
    class SearchScreenListWidget
            extends AlwaysSelectedEntryListWidget<SearchScreenListWidget.BlockEntry> {
        private void generateEntries() {
            blockUnderList.forEach((block) -> {
                String string = Registries.BLOCK.getId(block).toString();
                if (string.contains(input)) {
                    SearchScreen.SearchScreenListWidget.BlockEntry blockEntry = new SearchScreen.SearchScreenListWidget.BlockEntry(string);
                    this.addEntry(blockEntry);
                }
            });
            if (this.getSelectedOrNull() != null) {
                this.centerScrollOn(this.getSelectedOrNull());
            }
        }
        public SearchScreenListWidget(MinecraftClient client) {
            super(client, SearchScreen.this.width, SearchScreen.this.height - 93, 32, 18);
            generateEntries();
        }
        private void test() {
            this.clearEntries();
            generateEntries();
        }
        @Override
        protected int getScrollbarX() {
            return super.getScrollbarX() - 16;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }
        @Environment(value=EnvType.CLIENT)
        public class BlockEntry
                extends AlwaysSelectedEntryListWidget.Entry<SearchScreen.SearchScreenListWidget.BlockEntry> {
            private final String languageDefinition;

            public BlockEntry(String languageDefinition) {
                this.languageDefinition = languageDefinition;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                    context.drawCenteredTextWithShadow(SearchScreen.this.textRenderer, Text.literal(languageDefinition), SearchScreen.SearchScreenListWidget.this.width / 2, y + 1, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                this.onPressed();
                return true;
            }

            void onPressed() {
                textFieldWidget.setText(this.languageDefinition);
            }

            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.languageDefinition);
            }
        }
    }
}
