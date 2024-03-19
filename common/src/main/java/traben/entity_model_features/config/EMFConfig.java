package traben.entity_model_features.config;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import traben.entity_model_features.utils.EMFManager;
import traben.entity_model_features.utils.EMFOptiFinePartNameMappings;
import traben.entity_model_features.utils.OptifineMobNameForFileAndEMFMapId;
import traben.entity_texture_features.ETFApi;
import traben.entity_texture_features.ETFVersionDifferenceHandler;
import traben.entity_texture_features.config.ETFConfig;
import traben.tconfig.TConfig;
import traben.tconfig.gui.entries.*;

import java.util.*;

public class EMFConfig extends TConfig {

    public boolean logModelCreationData = false;
    public boolean debugOnRightClick = false;
    public RenderModeChoice renderModeChoice = RenderModeChoice.NORMAL;
    public VanillaModelRenderMode vanillaModelHologramRenderMode_2 = VanillaModelRenderMode.OFF;
    public boolean attemptRevertingEntityModelsAlteredByAnotherMod = true;
    public ModelPrintMode modelExportMode = ModelPrintMode.NONE;
    public PhysicsModCompatChoice attemptPhysicsModPatch_2 = PhysicsModCompatChoice.CUSTOM;
    public ETFConfig.UpdateFrequency modelUpdateFrequency = ETFConfig.UpdateFrequency.Average;

    public ETFConfig.String2EnumNullMap<RenderModeChoice> entityRenderModeOverrides = new ETFConfig.String2EnumNullMap<>();
    public ETFConfig.String2EnumNullMap<PhysicsModCompatChoice> entityPhysicsModPatchOverrides = new ETFConfig.String2EnumNullMap<>();
    public ETFConfig.String2EnumNullMap<VanillaModelRenderMode> entityVanillaHologramOverrides = new ETFConfig.String2EnumNullMap<>();
    public ObjectOpenHashSet<String> modelsNamesDisabled = new ObjectOpenHashSet<>();

    public boolean allowEBEModConfigModify = true;

    public int animationLODDistance = 20;

    public boolean retainDetailOnLowFps = true;

    public boolean retainDetailOnLargerMobs = true;

    @Override
    public TConfigEntryCategory getGUIOptions() {
        return new TConfigEntryCategory.Empty().add(
                new TConfigEntryCategory("config.entity_features.models_main").add(
                        new TConfigEntryCategory("entity_model_features.config.options", "entity_model_features.config.options.tooltip").add(
                                new TConfigEntryBoolean("entity_model_features.config.force_models", "entity_model_features.config.force_models.tooltip",
                                        () -> attemptRevertingEntityModelsAlteredByAnotherMod, value -> attemptRevertingEntityModelsAlteredByAnotherMod = value, true),
                                new TConfigEntryEnumButton<>("entity_model_features.config.physics", "entity_model_features.config.physics.tooltip",
                                        () -> attemptPhysicsModPatch_2, value -> attemptPhysicsModPatch_2 = value, PhysicsModCompatChoice.CUSTOM),
                                new TConfigEntryBoolean("entity_model_features.config.ebe_config_modify", "entity_model_features.config.ebe_config_modify.tooltip",
                                        () -> allowEBEModConfigModify, value -> allowEBEModConfigModify = value, true)
                        ),
                        new TConfigEntryCategory("entity_model_features.config.performance").add(
                                new TConfigEntryEnumSlider<>("entity_model_features.config.update", "entity_model_features.config.update.tooltip",
                                        () -> modelUpdateFrequency, value -> modelUpdateFrequency = value, ETFConfig.UpdateFrequency.Average),
                                new TConfigEntryInt("entity_model_features.config.lod", "entity_model_features.config.lod.tooltip",
                                        () -> animationLODDistance, value -> animationLODDistance = value, 20, 0, 65, true, true),
                                new TConfigEntryBoolean("entity_model_features.config.low_fps_lod", "entity_model_features.config.low_fps_lod.tooltip",
                                        () -> retainDetailOnLowFps, value -> retainDetailOnLowFps = value, true),
                                new TConfigEntryBoolean("entity_model_features.config.large_mob_lod", "entity_model_features.config.large_mob_lod.tooltip",
                                        () -> retainDetailOnLargerMobs, value -> retainDetailOnLargerMobs = value, true)
                        ),
                        new TConfigEntryCategory("entity_model_features.config.tools", "entity_model_features.config.tools.tooltip").add(
                                new TConfigEntryEnumSlider<>("entity_model_features.config.vanilla_render", "entity_model_features.config.vanilla_render.tooltip",
                                        () -> vanillaModelHologramRenderMode_2, value -> vanillaModelHologramRenderMode_2 = value, VanillaModelRenderMode.OFF),
                                new TConfigEntryEnumSlider<>("entity_model_features.config.print_mode", "entity_model_features.config.print_mode.tooltip",
                                        () -> modelExportMode, value -> modelExportMode = value, ModelPrintMode.NONE)
                        ),
                        new TConfigEntryCategory("entity_model_features.config.debug", "entity_model_features.config.debug.tooltip").add(
                                new TConfigEntryEnumSlider<>("entity_model_features.config.render", "entity_model_features.config.render.tooltip",
                                        () -> renderModeChoice, value -> renderModeChoice = value, RenderModeChoice.NORMAL),
                                new TConfigEntryBoolean("entity_model_features.config.log_models", "entity_model_features.config.log_models.tooltip",
                                        () -> logModelCreationData, value -> logModelCreationData = value, false),
                                new TConfigEntryBoolean("entity_model_features.config.debug_right_click", "entity_model_features.config.debug_right_click.tooltip",
                                        () -> debugOnRightClick, value -> debugOnRightClick = value, false)
                        ), getInfoSettings()
                )//, new TConfigEntryCategory("config.entity_features.general_settings.title")
                ,getEntitySettings()
        );
    }
    private TConfigEntryCategory getInfoSettings() {
        TConfigEntryCategory category = new TConfigEntryCategory("All models");
        for (Map.Entry<OptifineMobNameForFileAndEMFMapId, EntityModelLayer>  _in
                : EMFManager.getInstance().cache_LayersByModelName.entrySet()) {
            var vanilla = MinecraftClient.getInstance().getEntityModelLoader().modelParts.get(_in.getValue());
            if (vanilla != null) {
                var fileName = _in.getKey().getfileName();
                TConfigEntryCategory model = new TConfigEntryCategory(fileName+".jem");
                category.add(model);

                TConfigEntry export = getExport(_in);

                model.add(
                        new TConfigEntryBoolean("Enabled", "ltip",
                        () -> !modelsNamesDisabled.contains(fileName),
                                value -> {
                                    if (value) {
                                        modelsNamesDisabled.remove(fileName);
                                    } else {
                                        modelsNamesDisabled.add(fileName);
                                    }

                                },
                                true),
                        new TConfigEntryCategory("Model part names").addAll(
                                getmappings(_in.getKey().getMapId())
                        ),
                        export


                );
            }
        }



        return category;
    }

    @NotNull
    private TConfigEntry getExport(final Map.Entry<OptifineMobNameForFileAndEMFMapId, EntityModelLayer> _in) {
        TConfigEntry export;
        try {
            Objects.requireNonNull(_in.getKey().getMapId());
            Objects.requireNonNull(MinecraftClient.getInstance().getEntityModelLoader().modelParts.get(_in.getValue()));
            export = new TConfigEntryCustomButton("Export model", "jhv", (button) -> {
                var old = modelExportMode;
                modelExportMode = ModelPrintMode.ALL_LOG_AND_JEM;
                try{
                    EMFOptiFinePartNameMappings.getMapOf(_in.getKey().getMapId(),
                        MinecraftClient.getInstance().getEntityModelLoader().modelParts.get(_in.getValue()).createModel(),
                            false);
                }catch (Exception e) {
                    e.printStackTrace();
                }
                modelExportMode = old;
                button.active=false;
                button.setMessage(ETFVersionDifferenceHandler.getTextFromTranslation("Exported"));
            });
        } catch (Exception e) {
            export = new TConfigEntryText("cannot export model currently...");
        }
        return export;
    }


    private Collection<TConfigEntry> getmappings(String mapKey) {
        var list = new ArrayList<TConfigEntry>();
        Map<String,String> map;
        if (EMFOptiFinePartNameMappings.OPTIFINE_MODEL_MAP_CACHE.containsKey(mapKey)) {
            list.add(new TConfigEntryText("optifine part names:"));
            list.add(new TConfigEntryText("\\/"));
            map = EMFOptiFinePartNameMappings.OPTIFINE_MODEL_MAP_CACHE.get(mapKey);
        } else {
            list.add(new TConfigEntryText("un mapped part names:"));
            list.add(new TConfigEntryText("\\/"));
            map = EMFOptiFinePartNameMappings.UNKNOWN_MODEL_MAP_CACHE.get(mapKey);
        }
        if (map == null) {
            return List.of();
        }

        for (String entry : map.keySet()) {
            list.add(new TConfigEntryText(entry));
        }
        return list;
    }


    private TConfigEntryCategory getEntitySettings() {
        TConfigEntryCategory category = new TConfigEntryCategory("config.entity_features.per_entity_settings");

        try {
            Registries.ENTITY_TYPE.forEach((entityType) -> {
                //if (entityType != EntityType.PLAYER) {
                    String translationKey = entityType.getTranslationKey();
                    TConfigEntryCategory entityCategory = new TConfigEntryCategory(translationKey);
                    this.addEntityConfigs(entityCategory, translationKey);
                    category.add(entityCategory);
                //}
            });
            BlockEntityRendererFactories.FACTORIES.keySet().forEach((entityType) -> {
                String translationKey = ETFApi.getBlockEntityTypeToTranslationKey(entityType);
                TConfigEntryCategory entityCategory = (new TConfigEntryCategory(translationKey));
                this.addEntityConfigs(entityCategory, translationKey);
                category.add(entityCategory);
            });
        } catch (Exception var4) {
            //noinspection CallToPrintStackTrace
            var4.printStackTrace();
        }

        return category;
    }

    private void addEntityConfigs(TConfigEntryCategory entityCategory, String translationKey) {
        TConfigEntryCategory category = new TConfigEntryCategory("config.entity_features.models_main");
        entityCategory.add(category);
        category.add(
                new TConfigEntryEnumSlider<>("entity_model_features.config.render", "entity_model_features.config.render.tooltip",
                        () -> this.entityRenderModeOverrides.getNullable(translationKey),
                        (layer) -> this.entityRenderModeOverrides.putNullable(translationKey, layer),
                        null, RenderModeChoice.class),
                new TConfigEntryEnumButton<>("entity_model_features.config.vanilla_render", "entity_model_features.config.vanilla_render.tooltip",
                        () -> this.entityVanillaHologramOverrides.getNullable(translationKey),
                        (layer) -> this.entityVanillaHologramOverrides.putNullable(translationKey, layer),
                        null, VanillaModelRenderMode.class),
                new TConfigEntryEnumButton<>("entity_model_features.config.physics", "entity_model_features.config.physics.tooltip",
                        () -> this.entityPhysicsModPatchOverrides.getNullable(translationKey),
                        (layer) -> this.entityPhysicsModPatchOverrides.putNullable(translationKey, layer),
                        null, PhysicsModCompatChoice.class)
        );
    }

    @Override
    public Identifier getModIcon() {
        return new Identifier("entity_model_features", "textures/gui/icon.png");
    }


    public enum ModelPrintMode {
        NONE(ScreenTexts.OFF),
        @SuppressWarnings("unused")
        LOG_ONLY(Text.translatable("entity_model_features.config.print_mode.log")),
        LOG_AND_JEM(Text.translatable("entity_model_features.config.print_mode.log_jem")),
        @SuppressWarnings("unused")
        ALL_LOG_ONLY(Text.translatable("entity_model_features.config.print_mode.all_log")),
        ALL_LOG_AND_JEM(Text.translatable("entity_model_features.config.print_mode.all_log_jem"));

        private final Text text;

        ModelPrintMode(Text text) {
            this.text = text;
        }

        public boolean doesJems() {
            return this == LOG_AND_JEM || this == ALL_LOG_AND_JEM;
        }

        public boolean doesAll() {
            return this == ALL_LOG_ONLY || this == ALL_LOG_AND_JEM;
        }

        public boolean doesLog() {
            return this != NONE;
        }

        @Override
        public String toString() {
            return text.getString();
        }
    }

    public enum VanillaModelRenderMode {
        OFF(ScreenTexts.OFF),
        @SuppressWarnings("unused")
        NORMAL(Text.translatable("entity_model_features.config.vanilla_render.normal")),
        OFFSET(Text.translatable("entity_model_features.config.vanilla_render.offset"));

        private final Text text;

        VanillaModelRenderMode(Text text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text.getString();
        }
    }

    public enum PhysicsModCompatChoice {
        OFF(ScreenTexts.OFF),
        VANILLA(Text.translatable("entity_model_features.config.physics.1")),
        CUSTOM(Text.translatable("entity_model_features.config.physics.2"));

        private final Text text;

        PhysicsModCompatChoice(Text text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text.getString();
        }
    }

    public enum RenderModeChoice {
        NORMAL(Text.translatable("entity_model_features.config.render.normal")),
        GREEN(Text.translatable("entity_model_features.config.render.green")),
        LINES_AND_TEXTURE(Text.translatable("entity_model_features.config.render.lines_texture")),
        LINES_AND_TEXTURE_FLASH(Text.translatable("entity_model_features.config.render.lines_texture_flash")),
        LINES(Text.translatable("entity_model_features.config.render.lines")),
        NONE(Text.translatable("entity_model_features.config.render.none"));

        private final String text;


        RenderModeChoice(Text text) {
            this.text = text.getString();
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
