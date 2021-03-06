package hellfirepvp.astralsorcery.client.gui.container;

import hellfirepvp.astralsorcery.client.util.resource.AssetLibrary;
import hellfirepvp.astralsorcery.client.util.resource.AssetLoader;
import hellfirepvp.astralsorcery.client.util.resource.BindableResource;
import hellfirepvp.astralsorcery.common.container.ContainerJournal;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: GuiJournalContainer
 * Created by HellFirePvP
 * Date: 22.11.2016 / 14:38
 */
public class GuiJournalContainer extends GuiContainer {

    private static BindableResource texJournalContainer = AssetLibrary.loadTexture(AssetLoader.TextureLocation.GUI, "guiJStorageBook");

    public GuiJournalContainer(InventoryPlayer playerInv, ItemStack journal, int journalIndex) {
        super(new ContainerJournal(playerInv, journal, journalIndex));
    }

    @Override
    public void initGui() {
        this.xSize = 176;
        this.ySize = 166;
        super.initGui();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        texJournalContainer.bind();
        drawRect(guiLeft, guiTop, xSize, ySize);
    }

    protected void drawRect(int offsetX, int offsetY, int width, int height) {
        Tessellator tes = Tessellator.getInstance();
        VertexBuffer vb = tes.getBuffer();
        vb.begin(7, DefaultVertexFormats.POSITION_TEX);
        vb.pos(offsetX,         offsetY + height, zLevel).tex(0, 1).endVertex();
        vb.pos(offsetX + width, offsetY + height, zLevel).tex(1, 1).endVertex();
        vb.pos(offsetX + width, offsetY,          zLevel).tex(1, 0).endVertex();
        vb.pos(offsetX,         offsetY,          zLevel).tex(0, 0).endVertex();
        tes.draw();
    }

}
