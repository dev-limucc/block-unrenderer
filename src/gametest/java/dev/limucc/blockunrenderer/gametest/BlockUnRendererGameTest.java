package dev.limucc.blockunrenderer.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.blockunrenderer.BuildVariant;
import dev.limucc.blockunrenderer.ServerOptInConfig;
import dev.limucc.blockunrenderer.client.BlockUnRendererClient;
import dev.limucc.blockunrenderer.client.ModMenuIntegration;
import dev.limucc.blockunrenderer.client.compat.ScreenNav;
import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.gui.BlockManagerScreen;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import org.spongepowered.asm.mixin.MixinEnvironment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Runtime check of every Block UN-renderer feature, for BOTH build variants.
 *
 * <p>Modes (env BUR_TEST_MODE): {@code sp} (default) = full feature run in a singleplayer world;
 * {@code mp} = join a dedicated server (env BUR_SERVER=host:port, BUR_EXPECT=allowed|blocked) and verify that the
 * see-through permission gate behaves as expected for this build variant.
 *
 * <p>Run with {@code gradlew runClientGameTest "-Ptarget=<mc>" [-Pvariant=nopermission] [-Psodium=1] [-Pxaero=1]}.
 * The same scene is rendered with each feature off/on and compared pixel-wise (colour classifiers per object).
 */
public class BlockUnRendererGameTest implements FabricClientGameTest {

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("BUR-GAMETEST FAIL: " + what);
        System.out.println("[BUR-GAMETEST] PASS: " + what);
    }

    private static void info(String what) {
        System.out.println("[BUR-GAMETEST] INFO: " + what);
    }

    // ---- colour classifiers (daylight, LightMode OFF unless stated) --------------------------------
    private static boolean isRed(int rgb) {      // red concrete wall
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return r > 100 && g < 75 && b < 75 && r > 2 * g;
    }

    private static boolean isYellow(int rgb) {   // yellow shulker boxes
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return r > 170 && g > 120 && b < 90 && r > b * 3;
    }

    private static boolean isMagenta(int rgb) {  // magenta block displays (entities)
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return r > 110 && b > 100 && g < 90 && r > g + 50 && b > g + 50;
    }

    private static boolean isWater(int rgb) {    // blue water, only evaluated on the lower (ground) half
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return b > 130 && r < 120 && b > g + 25;
    }

    private static boolean isGrass(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return g > 110 && g > r + 25 && g > b + 40;
    }

    private static boolean isDirt(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return r > 80 && r < 200 && g > 50 && b < 110 && r > g + 15 && g > b + 15;
    }

    private static int count(BufferedImage img, double fy0, double fy1, IntPredicate p) {
        int w = img.getWidth(), h = img.getHeight();
        int n = 0;
        for (int y = (int) (h * fy0); y < (int) (h * fy1); y++) {
            for (int x = 0; x < w; x++) {
                if (p.test(img.getRGB(x, y))) n++;
            }
        }
        return n;
    }

    private static double meanLuma(BufferedImage img, double fy0, double fy1) {
        int w = img.getWidth(), h = img.getHeight();
        double sum = 0;
        long n = 0;
        for (int y = (int) (h * fy0); y < (int) (h * fy1); y += 2) {
            for (int x = 0; x < w; x += 2) {
                int p = img.getRGB(x, y);
                sum += 0.299 * ((p >> 16) & 255) + 0.587 * ((p >> 8) & 255) + 0.114 * (p & 255);
                n++;
            }
        }
        return sum / n;
    }

    private static BufferedImage shot(ClientGameTestContext ctx, String name) throws Exception {
        Path p = ctx.takeScreenshot(name);
        return ImageIO.read(p.toFile());
    }

    private static net.minecraft.world.level.block.Block red() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(net.minecraft.resources.Identifier.parse("minecraft:red_concrete")).orElseThrow();
    }

    private static void cmd(TestSingleplayerContext w, String c) {
        w.getServer().runCommand(c);
    }

    private static KeyMapping bind(ClientGameTestContext ctx, KeyMapping m, int key) {
        ctx.runOnClient(mc -> {
            m.setKey(keyboard().getOrCreate(key));
            KeyMapping.resetMapping();
        });
        return m;
    }

    /** InputConstants.Type.KEYSYM was renamed KEYBOARD in 26.3. */
    private static InputConstants.Type keyboard() {
        try {
            return InputConstants.Type.valueOf("KEYSYM");
        } catch (IllegalArgumentException e) {
            return InputConstants.Type.valueOf("KEYBOARD");
        }
    }

    private static void cfg(ClientGameTestContext ctx, java.util.function.Consumer<ModConfig> edit) {
        ctx.runOnClient(mc -> {
            ModConfig c = ConfigManager.get();
            edit.accept(c);
            HideState.rebuildFromConfig();
        });
    }

    private static void resetCfg(ClientGameTestContext ctx) {
        cfg(ctx, c -> {
            c.triggerMode = ModConfig.TriggerMode.TOGGLE;
            c.filterMode = ModConfig.FilterMode.HIDE_LISTED;
            c.hiddenBlocks.clear();
            c.hideEntities = false;
            c.hideLiquids = false;
            c.showBlocksUnderneath = true;
            c.lightMode = ModConfig.LightMode.OFF;
        });
    }

    private static void setActive(ClientGameTestContext ctx, boolean on) {
        ctx.runOnClient(mc -> HideState.setActive(on));
        ctx.waitTicks(45);
    }

    /** Names of merged mixin handler methods on a target class (proves the mixin really applied). */
    private static List<String> burMethods(String className) {
        try {
            Class<?> c = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return java.util.Arrays.stream(c.getDeclaredMethods()).map(java.lang.reflect.Method::getName)
                    .filter(n -> n.contains("bur$")).toList();
        } catch (Throwable t) {
            return List.of("<<" + t.getClass().getSimpleName() + ": " + t.getMessage() + ">>");
        }
    }

    private static void mixinProof(String label, String cls, boolean required) {
        List<String> m = burMethods(cls);
        boolean notLoaded = m.size() == 1 && m.get(0).startsWith("<<");
        if (!required && notLoaded) {
            info("soft mixin target not present (mod not loaded): " + label);
            return;
        }
        check(!m.isEmpty() && !notLoaded, "mixin applied on " + label + " -> " + m);
    }

    // ---- entry point -----------------------------------------------------------------------------
    @Override
    public void runTest(ClientGameTestContext ctx) throws RuntimeException {
        try {
            String mode = System.getenv().getOrDefault("BUR_TEST_MODE", "all");   // sp | mp | all
            info("variant=" + BuildVariant.NAME + " mode=" + mode
                    + " sodium=" + FabricLoader.getInstance().isModLoaded("sodium")
                    + " xaero=" + FabricLoader.getInstance().isModLoaded("xaerominimap"));
            ctx.runOnClient(mc -> MixinEnvironment.getCurrentEnvironment().audit());
            check(true, "MixinEnvironment.audit() passed (all mixins applied)");
            mixinProofs();
            if (!mode.equals("mp")) runSingleplayer(ctx);
            if (!mode.equals("sp")) runMultiplayer(ctx);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mixinProofs() {
        mixinProof("BlockStateBase (render shape / occlusion / light)", "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase", true);
        mixinProof("BlockEntityRenderDispatcher", "net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher", true);
        mixinProof("EntityRenderDispatcher", "net.minecraft.client.renderer.entity.EntityRenderDispatcher", true);
        mixinProof("FluidRenderer", "net.minecraft.client.renderer.block.FluidRenderer", true);
        mixinProof("Lightmap", "net.minecraft.client.renderer.Lightmap", true);
        boolean sodium = FabricLoader.getInstance().isModLoaded("sodium");
        mixinProof("Sodium DefaultFluidRenderer (soft compat)", "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", sodium);
        boolean xmini = FabricLoader.getInstance().isModLoaded("xaerominimap");
        mixinProof("Xaero MinimapWriter (soft guard)", "xaero.common.minimap.write.MinimapWriter", xmini);
        boolean xmap = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
        mixinProof("Xaero MapWriter (soft guard)", "xaero.map.MapWriter", xmap);
    }

    // ---- the shared scene (built from the server side) -------------------------------------------
    private static final String[] SCENE = {
            "fill 3 -60 9 5 -58 9 minecraft:red_concrete",
            "setblock 3 -60 5 minecraft:yellow_shulker_box",
            "setblock 4 -60 5 minecraft:yellow_shulker_box",
            "setblock 5 -60 5 minecraft:yellow_shulker_box",
            "fill -2 -61 3 2 -61 8 minecraft:water",
            "summon minecraft:block_display -5 -60 5 {block_state:{Name:\"minecraft:magenta_concrete\",id:\"minecraft:magenta_concrete\"}}",
            "summon minecraft:block_display -4 -60 5 {block_state:{Name:\"minecraft:magenta_concrete\",id:\"minecraft:magenta_concrete\"}}",
            "summon minecraft:block_display -3 -60 5 {block_state:{Name:\"minecraft:magenta_concrete\",id:\"minecraft:magenta_concrete\"}}",
    };

    // =============================== SINGLEPLAYER ==================================================
    private void runSingleplayer(ClientGameTestContext ctx) throws Exception {
        resetCfg(ctx);
        ctx.getInput().resizeWindow(1280, 720);
        ctx.waitTicks(5);
        KeyMapping toggleKey = bind(ctx, BlockUnRendererClient.TOGGLE_KEY, InputConstants.KEY_F8);
        KeyMapping openKey = bind(ctx, BlockUnRendererClient.OPEN_KEY, InputConstants.KEY_F9);

        try (TestSingleplayerContext w = ctx.worldBuilder().create()) {
            ctx.waitTicks(60);
            cmd(w, "gamemode creative @p");
            cmd(w, "difficulty peaceful");
            cmd(w, "time set noon");
            cmd(w, "weather clear");
            cmd(w, "gamerule advance_time false");
            for (String c : SCENE) cmd(w, c);
            cmd(w, "tp @p 0.5 -60 0.5 0 5");
            ctx.waitTicks(60);
            check(ctx.computeOnClient(mc -> HideState.isAllowed()), "singleplayer world: see-through allowed (" + BuildVariant.NAME + " build)");

            // ---- diagnostics: are the block-display entities present on both sides? ---------------------
            int serverDisplays = ctx.computeOnClient(mc -> {
                int n = 0;
                for (net.minecraft.world.entity.Entity e : mc.getSingleplayerServer().overworld().getAllEntities()) {
                    if (e instanceof net.minecraft.world.entity.Display.BlockDisplay) n++;
                }
                return n;
            });
            int clientDisplays = ctx.computeOnClient(mc -> {
                int n = 0;
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof net.minecraft.world.entity.Display.BlockDisplay d && d.blockRenderState() != null) n++;
                }
                return n;
            });
            info("block displays: server=" + serverDisplays + " client(with render state)=" + clientDisplays);
            info("display detail: " + ctx.computeOnClient(mc -> {
                StringBuilder sb = new StringBuilder();
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof net.minecraft.world.entity.Display.BlockDisplay d) {
                        sb.append("[tick=").append(d.tickCount).append(" state=").append(d.getBlockState().getBlock().getDescriptionId())
                          .append(" rs=").append(d.renderState() != null).append(" brs=").append(d.blockRenderState() != null)
                          .append(" pos=").append(d.blockPosition().toShortString()).append("] ");
                    }
                }
                return sb + " playerTick=" + mc.player.tickCount + " level=" + mc.level.getClass().getSimpleName();
            }));

            // ---- baseline ------------------------------------------------------------------------
            BufferedImage base = shot(ctx, "bur-01-baseline");
            int r0 = count(base, 0, 1, BlockUnRendererGameTest::isRed);
            int y0 = count(base, 0, 1, BlockUnRendererGameTest::isYellow);
            int m0 = count(base, 0, 1, BlockUnRendererGameTest::isMagenta);
            int w0 = count(base, 0.6, 1, BlockUnRendererGameTest::isWater);
            int g0 = count(base, 0.6, 1, BlockUnRendererGameTest::isGrass);
            info("baseline px: red=" + r0 + " yellow=" + y0 + " magenta=" + m0 + " water=" + w0 + " grass=" + g0);
            check(r0 > 2000 && y0 > 800 && m0 > 1500 && w0 > 1500 && g0 > 3000, "baseline scene renders all classes of object");

            // ---- 1. hide blocks (real keybind, toggle mode) + block entities (shulker boxes) ----------
            cfg(ctx, c -> {
                c.hiddenBlocks.add("minecraft:red_concrete");
                c.hiddenBlocks.add("minecraft:yellow_shulker_box");
            });
            ctx.getInput().pressKey(toggleKey);
            ctx.waitTicks(50);
            check(ctx.computeOnClient(mc -> HideState.isActive()), "1a toggle keybind activates hiding");
            BufferedImage hid = shot(ctx, "bur-02-blocks-hidden");
            int r1 = count(hid, 0, 1, BlockUnRendererGameTest::isRed);
            int y1 = count(hid, 0, 1, BlockUnRendererGameTest::isYellow);
            check(r1 < r0 * 0.1, "1b listed block (red concrete) vanished: " + r0 + " -> " + r1);
            check(y1 < y0 * 0.1, "1c listed BLOCK ENTITY (yellow shulker boxes) vanished: " + y0 + " -> " + y1);
            check(count(hid, 0, 1, BlockUnRendererGameTest::isMagenta) > m0 * 0.8, "1d unlisted entities untouched");
            check(count(hid, 0.6, 1, BlockUnRendererGameTest::isWater) > w0 * 0.8, "1e unlisted liquids untouched");
            check(ctx.computeOnClient(mc -> red().defaultBlockState().getRenderShape() == RenderShape.INVISIBLE),
                    "1f getRenderShape() mixin: red concrete is INVISIBLE while hiding");
            check(ctx.computeOnClient(mc -> Blocks.STONE.defaultBlockState().getRenderShape() == RenderShape.MODEL),
                    "1g unlisted block keeps its render shape");
            ctx.getInput().pressKey(toggleKey);
            ctx.waitTicks(50);
            check(ctx.computeOnClient(mc -> !HideState.isActive()), "1h toggle keybind again -> visible");
            BufferedImage back = shot(ctx, "bur-03-blocks-restored");
            check(count(back, 0, 1, BlockUnRendererGameTest::isRed) > r0 * 0.8
                    && count(back, 0, 1, BlockUnRendererGameTest::isYellow) > y0 * 0.8, "1i blocks + block entities are back");

            // ---- 2. filter mode SHOW_ONLY_LISTED -----------------------------------------------------
            cfg(ctx, c -> {
                c.hiddenBlocks.clear();
                c.hiddenBlocks.add("minecraft:red_concrete");
                c.filterMode = ModConfig.FilterMode.SHOW_ONLY_LISTED;
            });
            setActive(ctx, true);
            BufferedImage only = shot(ctx, "bur-04-show-only-listed");
            check(count(only, 0, 1, BlockUnRendererGameTest::isRed) > r0 * 0.7, "2a SHOW_ONLY: listed block still visible");
            check(count(only, 0.6, 1, BlockUnRendererGameTest::isGrass) < g0 * 0.1, "2b SHOW_ONLY: ground (grass) hidden: " + g0 + " -> " + count(only, 0.6, 1, BlockUnRendererGameTest::isGrass));
            check(count(only, 0, 1, BlockUnRendererGameTest::isYellow) < y0 * 0.1, "2c SHOW_ONLY: other block entities hidden");
            setActive(ctx, false);
            cfg(ctx, c -> c.filterMode = ModConfig.FilterMode.HIDE_LISTED);

            // ---- 3. liquids: toggle + listed fluid ---------------------------------------------------
            cfg(ctx, c -> {
                c.hiddenBlocks.clear();
                c.hideLiquids = true;
            });
            setActive(ctx, true);
            BufferedImage noWater = shot(ctx, "bur-05-liquids-hidden");
            int w1 = count(noWater, 0.6, 1, BlockUnRendererGameTest::isWater);
            check(w1 < w0 * 0.15, "3a 'Hide liquids' toggle: water vanished: " + w0 + " -> " + w1);
            check(count(noWater, 0, 1, BlockUnRendererGameTest::isRed) > r0 * 0.8, "3b blocks untouched by the liquids toggle");
            setActive(ctx, false);
            cfg(ctx, c -> {
                c.hideLiquids = false;
                c.hiddenBlocks.add("minecraft:water");
            });
            setActive(ctx, true);
            int w2 = count(shot(ctx, "bur-06-water-listed"), 0.6, 1, BlockUnRendererGameTest::isWater);
            check(w2 < w0 * 0.15, "3c water listed by id: vanished: " + w0 + " -> " + w2
                    + (FabricLoader.getInstance().isModLoaded("sodium") ? " (under SODIUM -> soft DefaultFluidRenderer mixin)" : " (vanilla FluidRenderer mixin)"));
            setActive(ctx, false);
            cfg(ctx, c -> c.hiddenBlocks.clear());

            // ---- 4. entities toggle -----------------------------------------------------------------
            cfg(ctx, c -> c.hideEntities = true);
            setActive(ctx, true);
            BufferedImage noEnt = shot(ctx, "bur-07-entities-hidden");
            int m1 = count(noEnt, 0, 1, BlockUnRendererGameTest::isMagenta);
            check(m1 < m0 * 0.1, "4a 'Hide entities': magenta block displays vanished: " + m0 + " -> " + m1);
            check(ctx.computeOnClient(mc -> HideState.shouldHideEntities()), "4b EntityRenderDispatcher gate reports hidden");
            check(count(noEnt, 0, 1, BlockUnRendererGameTest::isRed) > r0 * 0.8, "4c blocks untouched by the entities toggle");
            setActive(ctx, false);
            cfg(ctx, c -> c.hideEntities = false);
            check(count(shot(ctx, "bur-08-entities-back"), 0, 1, BlockUnRendererGameTest::isMagenta) > m0 * 0.8, "4d entities back when hiding is off");

            // ---- 5. fullbright ------------------------------------------------------------------------
            cmd(w, "time set midnight");
            ctx.waitTicks(30);
            cfg(ctx, c -> {
                c.hiddenBlocks.add("minecraft:red_concrete");
                c.lightMode = ModConfig.LightMode.OFF;
            });
            setActive(ctx, true);
            double lOff = meanLuma(shot(ctx, "bur-09-midnight-light-off"), 0.55, 1);
            cfg(ctx, c -> c.lightMode = ModConfig.LightMode.FULLBRIGHT);
            ctx.waitTicks(45);
            double lOn = meanLuma(shot(ctx, "bur-10-midnight-fullbright"), 0.55, 1);
            check(lOn > lOff * 1.25 && lOn - lOff > 20, "5a FULLBRIGHT lights the night scene: mean luma " + String.format("%.1f -> %.1f", lOff, lOn));
            check(ctx.computeOnClient(mc -> Blocks.STONE.defaultBlockState().getLightEmission() == 15), "5b getLightEmission() mixin = 15 under FULLBRIGHT");
            check(ctx.computeOnClient(mc -> red().defaultBlockState().propagatesSkylightDown()), "5c hidden block passes skylight under FULLBRIGHT");
            cfg(ctx, c -> c.lightMode = ModConfig.LightMode.OFF);
            check(ctx.computeOnClient(mc -> Blocks.STONE.defaultBlockState().getLightEmission() == 0), "5d light emission back to vanilla with Light OFF");
            setActive(ctx, false);
            cmd(w, "time set noon");
            ctx.waitTicks(30);

            // ---- 6. show blocks underneath -------------------------------------------------------------
            cfg(ctx, c -> {
                c.hiddenBlocks.clear();
                c.hiddenBlocks.add("minecraft:grass_block");
                c.showBlocksUnderneath = true;
                c.lightMode = ModConfig.LightMode.FULLBRIGHT;   // covered faces are unlit (black) without it
            });
            setActive(ctx, true);
            BufferedImage under = shot(ctx, "bur-11-underneath-on");
            int dirtOn = count(under, 0.6, 1, BlockUnRendererGameTest::isDirt);
            check(ctx.computeOnClient(mc -> !Blocks.GRASS_BLOCK.defaultBlockState().canOcclude()
                    && Blocks.GRASS_BLOCK.defaultBlockState().getFaceOcclusionShape(Direction.UP).isEmpty()),
                    "6a Underneath ON: hidden block stops occluding (canOcclude + face occlusion mixins)");
            cfg(ctx, c -> c.showBlocksUnderneath = false);
            ctx.waitTicks(45);
            BufferedImage noUnder = shot(ctx, "bur-12-underneath-off");
            int dirtOff = count(noUnder, 0.6, 1, BlockUnRendererGameTest::isDirt);
            check(ctx.computeOnClient(mc -> Blocks.GRASS_BLOCK.defaultBlockState().canOcclude()), "6b Underneath OFF: hidden block keeps occluding");
            check(dirtOn > 3000 && dirtOff < dirtOn * 0.5, "6c blocks underneath render ON vs hole OFF: dirt px " + dirtOn + " vs " + dirtOff);
            setActive(ctx, false);
            cfg(ctx, c -> {
                c.hiddenBlocks.clear();
                c.showBlocksUnderneath = true;
            });

            // ---- 7. HOLD trigger mode -------------------------------------------------------------------
            cfg(ctx, c -> {
                c.hiddenBlocks.add("minecraft:red_concrete");
                c.triggerMode = ModConfig.TriggerMode.HOLD;
            });
            ctx.getInput().holdKey(toggleKey);
            ctx.waitTicks(40);
            boolean heldActive = ctx.computeOnClient(mc -> HideState.isActive());
            int rHeld = count(shot(ctx, "bur-13-hold-key-down"), 0, 1, BlockUnRendererGameTest::isRed);
            ctx.getInput().releaseKey(toggleKey);
            ctx.waitTicks(45);
            check(heldActive, "7a HOLD mode: hidden while the key is held");
            check(rHeld < r0 * 0.1, "7b HOLD mode: red wall gone while held");
            check(ctx.computeOnClient(mc -> !HideState.isActive()), "7c HOLD mode: visible again after release");
            cfg(ctx, c -> {
                c.triggerMode = ModConfig.TriggerMode.TOGGLE;
                c.hiddenBlocks.clear();
            });

            // ---- 8. permission gate ------------------------------------------------------------------------
            cfg(ctx, c -> c.hiddenBlocks.add("minecraft:red_concrete"));
            setActive(ctx, true);
            ctx.runOnClient(mc -> HideState.setAllowed(false));   // what a disconnect / a server that never opted in does
            ctx.waitTicks(45);
            BufferedImage gate = shot(ctx, "bur-14-permission-revoked");
            int rGate = count(gate, 0, 1, BlockUnRendererGameTest::isRed);
            if (BuildVariant.NO_PERMISSION) {
                check(ctx.computeOnClient(mc -> HideState.isAllowed()), "8a NO-PERMISSION build: setAllowed(false) cannot revoke see-through");
                check(rGate < r0 * 0.1, "8b NO-PERMISSION build: still hiding: " + rGate);
            } else {
                check(ctx.computeOnClient(mc -> !HideState.isAllowed() && !HideState.shouldHide(red())),
                        "8a SECURE build: without opt-in the gate blocks every hide decision");
                check(rGate > r0 * 0.8, "8b SECURE build: red wall visible again while not allowed: " + rGate);
                ctx.runOnClient(mc -> HideState.setAllowed(true));  // == the server's opt-in handshake arriving
                ctx.waitTicks(45);
                int rBack = count(shot(ctx, "bur-15-permission-granted"), 0, 1, BlockUnRendererGameTest::isRed);
                if (rBack >= r0 * 0.1) {
                    info("8c diag: still visible after 45 ticks (" + rBack + "); active=" + ctx.computeOnClient(mc -> HideState.isActive())
                            + " allowed=" + ctx.computeOnClient(mc -> HideState.isAllowed())
                            + " shouldHide=" + ctx.computeOnClient(mc -> HideState.shouldHide(red()))
                            + " shape=" + ctx.computeOnClient(mc -> red().defaultBlockState().getRenderShape()));
                    ctx.waitTicks(120);
                    rBack = count(shot(ctx, "bur-15b-permission-granted-later"), 0, 1, BlockUnRendererGameTest::isRed);
                    info("8c diag: after +120 ticks: " + rBack);
                }
                check(rBack < r0 * 0.1, "8c SECURE build: opt-in handshake re-enables hiding");
            }
            setActive(ctx, false);
            cfg(ctx, c -> c.hiddenBlocks.clear());

            // ---- 9. GUI (Block Manager) -----------------------------------------------------------------------
            ctx.waitTicks(10);
            ctx.getInput().pressKey(openKey);
            boolean opened = waitScreen(ctx);
            if (!opened) {
                info("9a diag: open key=" + ctx.computeOnClient(mc -> openKey.saveString() + " isDown=" + openKey.isDown()
                        + " toggleKey=" + toggleKey.saveString() + " screen=" + ScreenNav.current(mc)
                        + " allowed=" + HideState.isAllowed() + " trigger=" + ConfigManager.get().triggerMode));
            }
            check(opened, "9a OPEN keybind opens the Block Manager screen");
            ctx.waitTicks(5);
            ctx.takeScreenshot("bur-16-gui-main");
            check(ctx.computeOnClient(mc -> ScreenNav.current(mc) instanceof BlockManagerScreen), "9a screen class = BlockManagerScreen");
            double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
            int sw = ctx.computeOnClient(mc -> ScreenNav.current(mc).width);
            int sh = ctx.computeOnClient(mc -> ScreenNav.current(mc).height);
            int cx = sw / 2, pl = cx - 155, col0 = pl + 76, col1 = pl + 157 + 76;

            ModConfig.FilterMode f0 = ctx.computeOnClient(mc -> ConfigManager.get().filterMode);
            click(ctx, col0, 44, scale);
            check(ctx.computeOnClient(mc -> ConfigManager.get().filterMode != f0), "9b Filter button flips HIDE/SHOW_ONLY");
            click(ctx, col0, 44, scale);
            ModConfig.TriggerMode t0 = ctx.computeOnClient(mc -> ConfigManager.get().triggerMode);
            click(ctx, col1, 44, scale);
            check(ctx.computeOnClient(mc -> ConfigManager.get().triggerMode != t0), "9c Trigger button flips TOGGLE/HOLD");
            click(ctx, col1, 44, scale);
            boolean e0 = ctx.computeOnClient(mc -> ConfigManager.get().hideEntities);
            click(ctx, col0, 82, scale);
            check(ctx.computeOnClient(mc -> ConfigManager.get().hideEntities != e0), "9d Entities toggle button");
            click(ctx, col0, 82, scale);
            boolean l0 = ctx.computeOnClient(mc -> ConfigManager.get().hideLiquids);
            click(ctx, col1, 82, scale);
            check(ctx.computeOnClient(mc -> ConfigManager.get().hideLiquids != l0), "9e Liquids toggle button");
            click(ctx, col1, 82, scale);
            boolean u0 = ctx.computeOnClient(mc -> ConfigManager.get().showBlocksUnderneath);
            click(ctx, col0, 106, scale);
            check(ctx.computeOnClient(mc -> ConfigManager.get().showBlocksUnderneath != u0), "9f Underneath toggle button");
            click(ctx, col0, 106, scale);
            ModConfig.LightMode lm0 = ctx.computeOnClient(mc -> ConfigManager.get().lightMode);
            click(ctx, col1, 106, scale);
            check(ctx.computeOnClient(mc -> ConfigManager.get().lightMode != lm0), "9g Light button flips OFF/FULLBRIGHT");
            click(ctx, col1, 106, scale);
            ctx.takeScreenshot("bur-17-gui-buttons");

            click(ctx, cx, 143, scale);           // focus the search box (the button clicks above moved the focus)
            ctx.getInput().typeChars("red_concrete");
            ctx.waitTicks(3);
            ctx.takeScreenshot("bur-18-gui-search");
            click(ctx, pl + 100, 168, scale);     // first result row
            check(ctx.computeOnClient(mc -> ConfigManager.get().hiddenBlocks.contains("minecraft:red_concrete")), "9h click a search row adds it to the list");
            ctx.waitTicks(3);
            ctx.takeScreenshot("bur-19-gui-row-added");
            click(ctx, pl + 100, 168, scale);
            check(ctx.computeOnClient(mc -> !ConfigManager.get().hiddenBlocks.contains("minecraft:red_concrete")), "9i click again removes it");

            click(ctx, pl + 30, sh - 18, scale);  // Info
            ctx.waitTicks(3);
            ctx.takeScreenshot("bur-20-gui-info");
            click(ctx, pl + 30, sh - 18, scale);  // Back
            click(ctx, cx, sh - 18, scale);       // Done
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ScreenNav.current(mc) == null), "9j Info/Back/Done buttons work (Done closes the screen)");
            ctx.setScreen(() -> new ModMenuIntegration().getModConfigScreenFactory().create(null));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ScreenNav.current(mc) instanceof BlockManagerScreen), "9k Mod Menu config entry opens the same screen");
            ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ScreenNav.current(mc) == null), "9l Escape closes it");
            System.out.println("[BUR-GAMETEST] PASS: ALL Block UN-renderer singleplayer checks complete (" + BuildVariant.NAME + ")");
        }
    }

    private static boolean waitScreen(ClientGameTestContext ctx) {
        try {
            ctx.waitFor(mc -> ScreenNav.current(mc) instanceof BlockManagerScreen, 30);
            return true;
        } catch (RuntimeException | AssertionError e) {
            return false;
        }
    }

    private static void click(ClientGameTestContext ctx, double guiX, double guiY, double scale) {
        ctx.getInput().setCursorPos(guiX * scale, guiY * scale);
        ctx.waitTick();
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(2);
    }

    // =============================== MULTIPLAYER (real dedicated server, same JVM) ===============
    /**
     * Joins a real dedicated server twice - once with the server opt-in OFF (the default) and once ON - and checks the
     * see-through permission gate: SECURE build = blocked without the opt-in, allowed with it; NO-PERMISSION build =
     * always allowed.
     */
    private void runMultiplayer(ClientGameTestContext ctx) throws Exception {
        resetCfg(ctx);
        ctx.getInput().resizeWindow(1280, 720);
        ctx.waitTicks(5);
        KeyMapping toggleKey = bind(ctx, BlockUnRendererClient.TOGGLE_KEY, InputConstants.KEY_F8);

        java.util.Properties props = new java.util.Properties();
        props.setProperty("level-type", "minecraft:flat");
        props.setProperty("spawn-protection", "0");
        try (TestDedicatedServerContext server = ctx.worldBuilder().createServer(props)) {
            for (String c : new String[]{"gamerule advance_time false", "time set noon", "weather clear", "difficulty peaceful"}) {
                server.runCommand(c);
            }
            for (String c : SCENE) server.runCommand(c);

            for (boolean serverOptIn : new boolean[]{false, true}) {
                // the server side reads this at JOIN time (ServerOptInConfig is the real server config class)
                ServerOptInConfig.get().allowSeeThrough = serverOptIn;
                boolean expectAllowed = BuildVariant.NO_PERMISSION || serverOptIn;
                info("MP: server opt-in=" + serverOptIn + ", expect see-through allowed=" + expectAllowed);
                resetCfg(ctx);
                try (var conn = server.connect()) {
                    ctx.waitFor(mc -> mc.level != null && mc.player != null && ScreenNav.current(mc) == null, 1200);
                    server.runCommand("gamemode creative @p");
                    server.runCommand("tp @p 0.5 -60 0.5 0 5");
                    ctx.waitTicks(80);   // chunks + the opt-in handshake packet
                    BufferedImage base = shot(ctx, "bur-mp-01-baseline-optin-" + serverOptIn);
                    int r0 = count(base, 0, 1, BlockUnRendererGameTest::isRed);
                    info("MP baseline red px = " + r0);
                    check(r0 > 3000, "MP optin=" + serverOptIn + ": dedicated-server scene visible (red wall)");

                    boolean allowedNow = ctx.computeOnClient(mc -> HideState.isAllowed());
                    check(allowedNow == expectAllowed, "MP optin=" + serverOptIn + ": permission state after join allowed="
                            + allowedNow + " (expected " + expectAllowed + ")");

                    cfg(ctx, c -> c.hiddenBlocks.add("minecraft:red_concrete"));
                    ctx.getInput().pressKey(toggleKey);
                    ctx.waitTicks(60);
                    boolean active = ctx.computeOnClient(mc -> HideState.isActive());
                    BufferedImage after = shot(ctx, "bur-mp-02-after-toggle-optin-" + serverOptIn);
                    int r1 = count(after, 0, 1, BlockUnRendererGameTest::isRed);
                    check(active == expectAllowed, "MP optin=" + serverOptIn + ": toggle keybind -> active=" + active
                            + " (expected " + expectAllowed + ")");
                    if (expectAllowed) {
                        check(r1 < r0 * 0.1, "MP optin=" + serverOptIn + ": see-through WORKS: red wall " + r0 + " -> " + r1);
                    } else {
                        check(r1 > r0 * 0.8, "MP optin=" + serverOptIn + ": see-through BLOCKED: red wall still visible " + r0 + " -> " + r1);
                    }
                    ctx.runOnClient(mc -> HideState.setActive(false));
                }
                ctx.waitTicks(20);
                // after leaving the server the gate closes again in the secure build
                check(ctx.computeOnClient(mc -> HideState.isAllowed()) == BuildVariant.NO_PERMISSION,
                        "MP optin=" + serverOptIn + ": after disconnect allowed=" + BuildVariant.NO_PERMISSION);
            }
        }
        System.out.println("[BUR-GAMETEST] PASS: ALL Block UN-renderer multiplayer checks complete (" + BuildVariant.NAME + ")");
    }
}
