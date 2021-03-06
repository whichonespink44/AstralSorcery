package hellfirepvp.astralsorcery.client.gui;

import hellfirepvp.astralsorcery.AstralSorcery;
import hellfirepvp.astralsorcery.client.sky.RenderAstralSkybox;
import hellfirepvp.astralsorcery.client.util.Blending;
import hellfirepvp.astralsorcery.client.util.RenderingUtils;
import hellfirepvp.astralsorcery.client.util.TextureHelper;
import hellfirepvp.astralsorcery.client.util.resource.AssetLibrary;
import hellfirepvp.astralsorcery.client.util.resource.BindableResource;
import hellfirepvp.astralsorcery.client.util.RenderConstellation;
import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.astralsorcery.common.constellation.IMajorConstellation;
import hellfirepvp.astralsorcery.common.constellation.distribution.ConstellationSkyHandler;
import hellfirepvp.astralsorcery.common.constellation.distribution.WorldSkyHandler;
import hellfirepvp.astralsorcery.common.constellation.star.StarConnection;
import hellfirepvp.astralsorcery.common.constellation.star.StarLocation;
import hellfirepvp.astralsorcery.common.data.DataActiveCelestials;
import hellfirepvp.astralsorcery.common.data.SyncDataHolder;
import hellfirepvp.astralsorcery.common.data.research.ResearchManager;
import hellfirepvp.astralsorcery.common.item.ItemConstellationPaper;
import hellfirepvp.astralsorcery.common.item.ItemJournal;
import hellfirepvp.astralsorcery.common.lib.ItemsAS;
import hellfirepvp.astralsorcery.common.network.PacketChannel;
import hellfirepvp.astralsorcery.common.network.packet.client.PktDiscoverConstellation;
import hellfirepvp.astralsorcery.client.util.resource.AssetLoader;
import hellfirepvp.astralsorcery.common.tile.TileTelescope;
import hellfirepvp.astralsorcery.common.util.ItemUtils;
import hellfirepvp.astralsorcery.common.util.data.Tuple;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: GuiTelescope
 * Created by HellFirePvP
 * Date: 08.05.2016 / 23:51
 */
public class GuiTelescope extends GuiWHScreen {

    private static final BindableResource textureGrid = AssetLibrary.loadTexture(AssetLoader.TextureLocation.GUI, "gridTelescope");
    private static final BindableResource textureConnection = AssetLibrary.loadTexture(AssetLoader.TextureLocation.EFFECT, "connectionPerks");

    private final EntityPlayer owningPlayer;
    private final TileTelescope guiOwner;

    private CellRenderInformation currentInformation = null;

    public GuiTelescope(EntityPlayer player, TileTelescope e) {
        super(245, 500);
        this.owningPlayer = player;
        this.guiOwner = e;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        drawWHRect(textureGrid);
        TextureHelper.refreshTextureBindState();

        zLevel -= 5;
        drawCellsWithEffects(partialTicks);
        zLevel += 5;
        TextureHelper.refreshTextureBindState();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void drawCellsWithEffects(float partialTicks) {
        WorldSkyHandler handle = ConstellationSkyHandler.getInstance().getWorldHandler(guiOwner.getWorld());
        int lastTracked = handle == null ? 5 : handle.lastRecordedDay;
        Random r = new Random(guiOwner.getWorld().getSeed() * 31 + lastTracked * 31);
        World world = Minecraft.getMinecraft().world;
        boolean canSeeSky = canTelescopeSeeSky(world);

        /*Collection<IConstellation> activeConstellations =
                ((DataActiveCelestials) SyncDataHolder.getDataClient(SyncDataHolder.DATA_CONSTELLATIONS)).getActiveConstellations();
        IConstellation[] constellations = evaluateConstellations(r, activeConstellations);*/
        IConstellation[] constellations = new IConstellation[8];
        if(handle != null) {
            LinkedList<IConstellation> active = handle.getSortedActiveConstellations();
            Iterator<IConstellation> iterator = active.iterator();
            while (iterator.hasNext()) {
                IConstellation c = iterator.next();
                if(!(c instanceof IMajorConstellation)) {
                    iterator.remove();
                    continue; //Telescope ignores non-major constellations
                }
                if(handle.getCurrentDistribution((IMajorConstellation) c, (f) -> f) <= 0.5F) {
                    iterator.remove();
                }
            }
            if(active.size() <= 8) {
                active.toArray(constellations);
            } else {
                active.subList(0, 8).toArray(constellations);
            }
        }

        GL11.glEnable(GL11.GL_BLEND);
        Blending.DEFAULT.apply();

        drawGridBackground(partialTicks, canSeeSky);

        Map<Rectangle, Integer> cells = new HashMap<>();
        Map<Integer, ConstellationInformation> starRectangles = new HashMap<>();

        if (handle != null && canSeeSky) {
            int wstep = guiWidth / 4;
            int hstep = guiHeight / 2;
            int cnt = 0;
            List<Tuple<Integer, Integer>> boxes = new LinkedList<>();
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 2; z++) {
                    boxes.add(new Tuple<>(x, z));
                }
            }
            Collections.shuffle(boxes, r);
            for (Tuple<Integer, Integer> t : boxes) {
                int x = t.key;
                int z = t.value;
                int offsetX = guiLeft + x * wstep;
                int offsetZ = guiTop + z * hstep;
                zLevel += 1;
                Optional<Map<StarLocation, Rectangle>> stars = drawCellEffects(r, constellations[cnt], offsetX, offsetZ, wstep, hstep, partialTicks);
                zLevel -= 1;

                cells.put(new Rectangle(offsetX, offsetZ, wstep, hstep), cnt);
                if (stars.isPresent()) {
                    ConstellationInformation info = new ConstellationInformation(stars.get(), constellations[cnt]);
                    starRectangles.put(cnt, info);
                }

                cnt++;
            }
        }

        zLevel += 2;
        drawDrawnLines(r, partialTicks);
        zLevel -= 2;

        currentInformation = new CellRenderInformation(cells, starRectangles);

        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawDrawnLines(final Random r, final float pTicks) {
        if (!canStartDrawing()) {
            if (currentLinesCell != -1) {
                clearLines();
                abortDrawing();
            }
            return;
        }

        float linebreadth = 2F;
        RenderConstellation.BrightnessFunction func = new RenderConstellation.BrightnessFunction() {
            @Override
            public float getBrightness() {
                return RenderConstellation.conCFlicker(Minecraft.getMinecraft().world.getWorldTime(), pTicks, 5 + r.nextInt(15));
            }
        };

        textureConnection.bind();

        for (int j = 0; j < 2; j++) {
            for (Line l : drawnLines) {
                drawLine(l.start, l.end, func, linebreadth, true);
            }

            if (currentDrawCell != -1 && start != null && end != null) {
                Point adjStart = new Point(start.x - guiLeft, start.y - guiTop);
                Point adjEnd = new Point(end.x - guiLeft, end.y - guiTop);
                drawLine(adjStart, adjEnd, func, linebreadth, false);
            }
        }
    }

    private void drawLine(Point start, Point end, RenderConstellation.BrightnessFunction func, float linebreadth, boolean applyFunc) {
        Tessellator tes = Tessellator.getInstance();
        VertexBuffer vb = tes.getBuffer();

        float brightness;
        if (applyFunc) {
            brightness = func.getBrightness();
        } else {
            brightness = 1F;
        }
        float starBr = Minecraft.getMinecraft().world.getStarBrightness(1.0F);
        if (starBr <= 0.0F) {
            return;
        }
        brightness *= (starBr * 2);
        vb.begin(7, DefaultVertexFormats.POSITION_TEX);
        GL11.glColor4f(brightness, brightness, brightness, brightness < 0 ? 0 : brightness);

        Vector3 fromStar = new Vector3(guiLeft + start.getX(), guiTop + start.getY(), zLevel);
        Vector3 toStar = new Vector3(guiLeft + end.getX(), guiTop + end.getY(), zLevel);

        Vector3 dir = toStar.clone().subtract(fromStar);
        Vector3 degLot = dir.clone().crossProduct(new Vector3(0, 0, 1)).normalize().multiply(linebreadth);//.multiply(j == 0 ? 1 : -1);

        Vector3 vec00 = fromStar.clone().add(degLot);
        Vector3 vecV = degLot.clone().multiply(-2);

        for (int i = 0; i < 4; i++) {
            int u = ((i + 1) & 2) >> 1;
            int v = ((i + 2) & 2) >> 1;

            Vector3 pos = vec00.clone().add(dir.clone().multiply(u)).add(vecV.clone().multiply(v));
            vb.pos(pos.getX(), pos.getY(), pos.getZ()).tex(u, v).endVertex();
        }

        tes.draw();
    }

    private void drawGridBackground(float partialTicks, boolean canSeeSky) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        World renderWorld = Minecraft.getMinecraft().world;
        int rgbFrom, rgbTo;
        if (canSeeSky) {
            float starBr = renderWorld.getStarBrightness(partialTicks) * 2;
            rgbFrom = calcRGBFrom(starBr);
            rgbTo = calcRGBTo(starBr);
        } else {
            rgbFrom = 0x000000;
            rgbTo = 0x000000;
        }
        int alphaMask = 0xFF000000; //100% opacity.
        RenderingUtils.drawGradientRect(guiLeft + 4, guiTop + 4, zLevel, guiLeft + guiWidth - 4, guiTop + guiHeight - 4, new Color(alphaMask | rgbFrom), new Color(alphaMask | rgbTo));
        GL11.glPopAttrib();
    }

    private boolean canTelescopeSeeSky(World renderWorld) {
        BlockPos pos = guiOwner.getPos();
        /*int height = 1;
        IBlockState up = renderWorld.getBlockState(pos.up());
        if(up.getBlock().equals(BlocksAS.blockStructural) && up.getValue(BlockStructural.BLOCK_TYPE).equals(BlockStructural.BlockType.TELESCOPE_STRUCT)) {
            height += 1;
        }*/

        for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
                BlockPos other = pos.add(xx, 0, zz);
                if (!renderWorld.canSeeSky(other)) {
                    return false;
                }
            }
        }
        return renderWorld.canSeeSky(pos.up());
    }

    private static final float THRESHOLD_TO_START = 0.8F;
    private static final float THRESHOLD_TO_SHIFT_BLUEGRAD = 0.5F;
    private static final float THRESHOLD_TO_MAX_BLUEGRAD = 0.2F;

    private int calcRGBTo(float starBr) {
        if (starBr >= THRESHOLD_TO_START) { //Blue ranges from 0 (1.0F starBr) to 170 (0.7F starBr)
            return 0; //Black.
        } else if (starBr >= THRESHOLD_TO_SHIFT_BLUEGRAD) { //Blue ranges from 170/AA (0.7F) to 255 (0.4F), green from 0 (0.7F) to 85(0.4F)
            float partSize = (THRESHOLD_TO_START - THRESHOLD_TO_SHIFT_BLUEGRAD);
            float perc = 1F - (starBr - THRESHOLD_TO_SHIFT_BLUEGRAD) / partSize;
            return (int) (perc * 170F);
        } else if (starBr >= THRESHOLD_TO_MAX_BLUEGRAD) {
            float partSize = (THRESHOLD_TO_SHIFT_BLUEGRAD - THRESHOLD_TO_MAX_BLUEGRAD);
            float perc = 1F - (starBr - THRESHOLD_TO_MAX_BLUEGRAD) / partSize;
            int green = (int) (perc * 85F);
            int blue = green + 0xAA; //LUL
            return (green << 8) | blue;
        } else {
            float partSize = (THRESHOLD_TO_MAX_BLUEGRAD - 0.0F); //Blue is 255, green from 85 (0.4F) to 175 (0.0F)
            float perc = 1F - (starBr - 0) / partSize;
            int green = 85 + ((int) (perc * 90));
            int red = (int) (perc * 140);
            return (red << 16) | (green << 8) | 0xFF;
        }
    }

    private static final float THRESHOLD_FROM_START = 1.0F;
    private static final float THRESHOLD_FROM_SHIFT_BLUEGRAD = 0.6F;
    private static final float THRESHOLD_FROM_MAX_BLUEGRAD = 0.3F;

    private int calcRGBFrom(float starBr) {
        if (starBr >= THRESHOLD_FROM_START) { //Blue ranges from 0 (1.0F starBr) to 170 (0.7F starBr)
            return 0; //Black.
        } else if (starBr >= THRESHOLD_FROM_SHIFT_BLUEGRAD) { //Blue ranges from 170/AA (0.7F) to 255 (0.4F), green from 0 (0.7F) to 85(0.4F)
            float partSize = (THRESHOLD_FROM_START - THRESHOLD_FROM_SHIFT_BLUEGRAD);
            float perc = 1F - (starBr - THRESHOLD_FROM_SHIFT_BLUEGRAD) / partSize;
            return (int) (perc * 170F);
        } else if (starBr >= THRESHOLD_FROM_MAX_BLUEGRAD) {
            float partSize = (THRESHOLD_FROM_SHIFT_BLUEGRAD - THRESHOLD_FROM_MAX_BLUEGRAD);
            float perc = 1F - (starBr - THRESHOLD_FROM_MAX_BLUEGRAD) / partSize;
            int green = (int) (perc * 85F);
            int blue = green + 0xAA; //LUL
            return (green << 8) | blue;
        } else {
            float partSize = (THRESHOLD_FROM_MAX_BLUEGRAD - 0.0F); //Blue is 255, green from 85 (0.4F) to 175 (0.0F)
            float perc = 1F - (starBr - 0) / partSize;
            int green = 85 + ((int) (perc * 90));
            int red = (int) (perc * 140);
            return (red << 16) | (green << 8) | 0xFF;
        }
    }

    private Optional<Map<StarLocation, Rectangle>> drawCellEffects(final Random rand, IConstellation c, int offsetX, int offsetY, int width, int height, final float partialTicks) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        Blending.DEFAULT.apply();

        RenderAstralSkybox.TEX_STAR_1.bind();
        int starSize = 2;
        for (int i = 0; i < 9 + rand.nextInt(18); i++) {
            int innerOffsetX = starSize + rand.nextInt(width - starSize);
            int innerOffsetY = starSize + rand.nextInt(height - starSize);
            GL11.glPushMatrix();
            float brightness = RenderConstellation.stdFlicker(owningPlayer.world.getWorldTime(), partialTicks, 10 + rand.nextInt(20));
            brightness *= Minecraft.getMinecraft().world.getStarBrightness(1.0F) * 2;
            GL11.glColor4f(brightness, brightness, brightness, brightness);
            drawRect(offsetX + innerOffsetX - starSize, offsetY + innerOffsetY - starSize, starSize * 2, starSize * 2);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glPopMatrix();
        }

        Map<StarLocation, Rectangle> rectangles = null;
        if (c != null) {
            zLevel += 1;

            int wPart = ((int) (((float) width) * 0.1F));
            int hPart = ((int) (((float) height) * 0.1F));

            rectangles = RenderConstellation.renderConstellationIntoGUI(
                    c,
                    offsetX + wPart,
                    offsetY + hPart,
                    zLevel,
                    width - (((int) (wPart * 1.5F))),
                    height - (((int) (hPart * 1.5F))),
                    2,
                    new RenderConstellation.BrightnessFunction() {
                        @Override
                        public float getBrightness() {
                            return RenderConstellation.conCFlicker(Minecraft.getMinecraft().world.getWorldTime(), partialTicks, 5 + rand.nextInt(15));
                        }
                    },
                    ResearchManager.clientProgress.hasConstellationDiscovered(c.getUnlocalizedName()),
                    true
            );

            zLevel -= 1;
        }

        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopAttrib();
        return Optional.ofNullable(rectangles);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            tryStartDrawing(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0) {
            informMovement(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            informRelease(mouseX, mouseY);
        }
    }

    private int currentLinesCell = -1;
    private LinkedList<Line> drawnLines = new LinkedList<>();

    private int currentDrawCell = -1;
    private Point start, end;

    private void tryStartDrawing(int mouseX, int mouseY) {
        if (currentDrawCell != -1) return;

        if (!canStartDrawing()) return;

        int cell = findCurrentCell(mouseX, mouseY);
        if (currentLinesCell == -1) {
            if (cell != -1) {
                currentDrawCell = cell;
                currentLinesCell = cell;
                start = new Point(mouseX, mouseY);
                end = new Point(mouseX, mouseY); //We want 2 different objects here though.
            }
        } else {
            if (cell != currentLinesCell) {
                abortDrawing();
                clearLines();

                currentDrawCell = cell;
                currentLinesCell = cell;
                start = new Point(mouseX, mouseY);
                end = new Point(mouseX, mouseY); //We want 2 different objects here though.
            } else {
                currentDrawCell = cell;
                start = new Point(mouseX, mouseY);
                end = new Point(mouseX, mouseY); //We want 2 different objects here though.
            }
        }
    }

    private boolean canStartDrawing() {
        return Minecraft.getMinecraft().world.getStarBrightness(1.0F) >= 0.35F;
    }

    private void clearLines() {
        currentLinesCell = -1;
        drawnLines.clear();
    }

    private int findCurrentCell(int x, int y) {
        CellRenderInformation current = currentInformation;
        for (Rectangle r : current.cells.keySet()) {
            if (r.contains(x, y)) {
                return current.cells.get(r);
            }
        }
        return -1;
    }

    private void informMovement(int mouseX, int mouseY) {
        if (currentDrawCell == -1) return;

        int cCell = findCurrentCell(mouseX, mouseY);
        if (cCell != currentDrawCell) {
            abortDrawing();
        } else {
            end = new Point(mouseX, mouseY);
        }
    }

    private void informRelease(int mouseX, int mouseY) {
        if (currentDrawCell == -1) return;

        int cCell = findCurrentCell(mouseX, mouseY);
        if (cCell != currentDrawCell) {
            abortDrawing();
        } else {
            end = new Point(mouseX, mouseY);
            pushDrawnLine(start, end);
            abortDrawing();

            checkConstellation(currentLinesCell, drawnLines);
        }
    }

    private void checkConstellation(int currentLinesCell, List<Line> drawnLines) {
        if (!currentInformation.starRectangleMap.containsKey(currentLinesCell)) return;

        ConstellationInformation constellationInfo = currentInformation.starRectangleMap.get(currentLinesCell);
        IConstellation c = constellationInfo.constellation;
        if (c == null || ResearchManager.clientProgress.hasConstellationDiscovered(c.getUnlocalizedName())) return;

        boolean has = false;
        IItemHandler handle = Minecraft.getMinecraft().player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        List<ItemStack> papers = ItemUtils.scanInventoryFor(handle, ItemsAS.constellationPaper);
        for (ItemStack stack : papers) {
            IConstellation con = ItemConstellationPaper.getConstellation(stack);
            if(con.equals(c)) {
                has = true;
                break;
            }
        }
        if(!has) {
            List<ItemStack> journals = ItemUtils.scanInventoryFor(handle, ItemsAS.journal);
            lblJournals:
            for (ItemStack stack : journals) {
                for (IConstellation con : ItemJournal.getStoredConstellations(stack)) {
                    if(con.equals(c)) {
                        has = true;
                        break lblJournals;
                    }
                }
            }
        }

        if(!has) return;

        List<StarConnection> sc = c.getStarConnections();
        if (sc.size() != drawnLines.size()) return; //Can't match otherwise anyway.

        Map<StarLocation, Rectangle> stars = constellationInfo.starRectangles;

        for (StarConnection connection : sc) {
            Rectangle fromRect = stars.get(connection.from);
            if (fromRect == null) {
                AstralSorcery.log.info("Could not check constellation of telescope drawing - starLocation is missing?");
                return;
            }
            Rectangle toRect = stars.get(connection.to);
            if (toRect == null) {
                AstralSorcery.log.info("Could not check constellation of telescope drawing - starLocation is missing?");
                return;
            }
            if (!containsMatch(drawnLines, fromRect, toRect)) {
                return;
            }
        }

        //We found a match. horray.
        PacketChannel.CHANNEL.sendToServer(new PktDiscoverConstellation(c.getUnlocalizedName()));
        clearLines();
        abortDrawing();
    }

    private boolean containsMatch(List<Line> drawnLines, Rectangle r1, Rectangle r2) {
        for (Line l : drawnLines) {
            Point start = l.start;
            Point end = l.end;
            start = new Point(start.x + guiLeft, start.y + guiTop);
            end = new Point(end.x + guiLeft, end.y + guiTop);
            if ((r1.contains(start) && r2.contains(end)) ||
                    (r2.contains(start) && r1.contains(end))) {
                return true;
            }
        }
        return false;
    }

    private void pushDrawnLine(Point start, Point end) {
        if (Math.abs(start.getX() - end.getX()) <= 2 &&
                Math.abs(start.getY() - end.getY()) <= 2) {
            return; //Rather a point than a line. probably not the users intention...
        }
        Point adjStart = new Point(start.x - guiLeft, start.y - guiTop);
        Point adjEnd = new Point(end.x - guiLeft, end.y - guiTop);
        Line l = new Line(adjStart, adjEnd);
        this.drawnLines.addLast(l);
    }

    private void abortDrawing() {
        currentDrawCell = -1;
        start = null;
        end = null;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class CellRenderInformation {

        private final Map<Rectangle, Integer> cells;
        private final Map<Integer, ConstellationInformation> starRectangleMap;

        public CellRenderInformation(Map<Rectangle, Integer> cells, Map<Integer, ConstellationInformation> starRectangleMap) {
            this.cells = cells;
            this.starRectangleMap = starRectangleMap;
        }
    }

    public static class ConstellationInformation {

        private final Map<StarLocation, Rectangle> starRectangles;
        private final IConstellation constellation;

        public ConstellationInformation(Map<StarLocation, Rectangle> starRectangles, IConstellation c) {
            this.starRectangles = starRectangles;
            this.constellation = c;
        }
    }

    public static class Line {

        public final Point start, end;

        public Line(Point start, Point end) {
            this.start = start;
            this.end = end;
        }
    }

}
