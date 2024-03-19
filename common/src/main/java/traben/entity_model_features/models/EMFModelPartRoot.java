package traben.entity_model_features.models;


import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import traben.entity_model_features.EMF;
import traben.entity_model_features.config.EMFConfig;
import traben.entity_model_features.models.animation.EMFAnimation;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.jem_objects.EMFJemData;
import traben.entity_model_features.models.jem_objects.EMFPartData;
import traben.entity_model_features.utils.EMFManager;
import traben.entity_model_features.utils.EMFUtils;
import traben.entity_model_features.utils.OptifineMobNameForFileAndEMFMapId;
import traben.entity_texture_features.ETFApi;
import traben.entity_texture_features.features.property_reading.PropertiesRandomProvider;
import traben.entity_texture_features.utils.EntityIntLRU;

import java.util.*;

import static traben.entity_model_features.utils.EMFManager.getJemDataWithDirectory;


@Environment(value = EnvType.CLIENT)
public class EMFModelPartRoot extends EMFModelPartVanilla {

    public final OptifineMobNameForFileAndEMFMapId modelName;
    public final ModelPart vanillaRoot;
    public final @NotNull EntityIntLRU entitySuffixMap = new EntityIntLRU();
    private final Map<String, EMFModelPartVanilla> allVanillaParts;
    private final Int2ObjectOpenHashMap<ModelPart> vanillaFormatModelPartOfEachState = new Int2ObjectOpenHashMap<>();
    public EMFManager.CemDirectoryApplier variantDirectoryApplier;
    public ETFApi.ETFVariantSuffixProvider variantTester = null;
    public boolean containsCustomModel = false;
    private long lastMobCountAnimatedOn = 0;
    private boolean hasRemovedTopLevelJemTextureFromChildren = false;

    //construct vanilla root
    public EMFModelPartRoot(OptifineMobNameForFileAndEMFMapId mobNameForFileAndMap,
                            EMFManager.CemDirectoryApplier variantDirectoryApplier,
                            ModelPart vanillaRoot,
                            Collection<String> optifinePartNames,
                            Map<String, EMFModelPartVanilla> mapForCreatedParts) {
        //create vanilla root model object
        super("EMF_root", vanillaRoot, optifinePartNames, mapForCreatedParts);
        allVanillaParts = mapForCreatedParts;
        allVanillaParts.put(name, this);

        this.modelName = mobNameForFileAndMap;
        this.variantDirectoryApplier = variantDirectoryApplier;

        this.vanillaRoot = vanillaRoot;

        //init the first time runnable into all vanilla children
        receiveOneTimeRunnable(this::registerModelRunnableWithEntityTypeContext);
    }

    private void registerModelRunnableWithEntityTypeContext() {
        if (EMFAnimationEntityContext.getEMFEntity() != null) {

            //register models to entity type for debug print
            if (EMF.config().getConfig().debugOnRightClick) {
                String type = EMFAnimationEntityContext.getEMFEntity().emf$getTypeString();
                Set<EMFModelPartRoot> roots = EMFManager.getInstance().rootPartsPerEntityTypeForDebug.get(type);
                if (roots == null) {
                    Set<EMFModelPartRoot> newRootSet = new ObjectLinkedOpenHashSet<>();
                    EMFManager.getInstance().rootPartsPerEntityTypeForDebug.put(type, newRootSet);
                    newRootSet.add(this);
                } else {
                    roots.add(this);
                }
            }

            //register variant runnable
            if (this.variantTester != null) {
                String type = EMFAnimationEntityContext.getEMFEntity().emf$getTypeString();
                if (EMF.config().getConfig().logModelCreationData)
                    EMFUtils.log("Registered new variating model for: " + type);

                Set<Runnable> variators = EMFManager.getInstance().rootPartsPerEntityTypeForVariation.get(type);
                if (variators == null) {
                    Set<Runnable> newVariators = new HashSet<>();
                    EMFManager.getInstance().rootPartsPerEntityTypeForVariation.put(type, newVariators);
                    newVariators.add(this::doVariantCheck);
                } else {
                    variators.add(this::doVariantCheck);
                }
            }
        }
        //now set the runnable to null so it only runs once
        this.receiveOneTimeRunnable(null);
    }

    public void doVariantCheck() {
        int finalSuffix;
        UUID id = EMFAnimationEntityContext.getEMFEntity().etf$getUuid();
        int knownSuffix = entitySuffixMap.getInt(id);
        if (knownSuffix != -1) {
            checkIfShouldExpireEntity(id);
            finalSuffix = knownSuffix;
        } else {
            int newSuffix;
            newSuffix = this.variantTester.getSuffixForETFEntity(EMFAnimationEntityContext.getEMFEntity());
            if (newSuffix == 0) {//DONT ALLOW 0 IN EMF
                this.entitySuffixMap.put(id, 1);
                finalSuffix = 1;
            } else {
                this.entitySuffixMap.put(id, newSuffix);
                finalSuffix = newSuffix;
            }
        }
        setVariantStateTo(finalSuffix);
    }

    public void checkIfShouldExpireEntity(UUID id) {
        if (this.variantTester.entityCanUpdate(id)) {
            switch (EMF.config().getConfig().modelUpdateFrequency) {
                case Never -> {
                }
                case Instant -> this.entitySuffixMap.removeInt(id);
                default -> {
                    int delay = EMF.config().getConfig().modelUpdateFrequency.getDelay();
                    int time = (int) (EMFAnimationEntityContext.getTime() % delay);
                    if (time == Math.abs(id.hashCode()) % delay) {
                        this.entitySuffixMap.removeInt(id);
                    }
                }
            }
        }
    }

    //root only
    public void addVariantOfJem(EMFJemData jemData, int variant) {
        if (EMF.config().getConfig().logModelCreationData)
            EMFUtils.log(" > " + jemData.getMobModelIDInfo().getfileName() + ", constructing variant #" + variant);

        Map<String, EMFModelPartCustom> newEmfParts = new HashMap<>();
        for (EMFPartData part :
                jemData.models) {
            if (part.part != null) {
                String idUnique = EMFUtils.getIdUnique(newEmfParts.keySet(), part.id);
                newEmfParts.put(idUnique, new EMFModelPartCustom(part, variant, part.part, idUnique));
            }
        }
        for (Map.Entry<String, EMFModelPartVanilla> vanillaEntry :
                allVanillaParts.entrySet()) {
            String thisPartName = vanillaEntry.getKey();
            EMFModelPartVanilla thisPart = vanillaEntry.getValue();
            EMFModelState vanillaState = EMFModelState.copy(thisPart.allKnownStateVariants.get(0));
            thisPart.setFromState(vanillaState);
            if (thisPart instanceof EMFModelPartRoot root && !root.cuboids.isEmpty()) {
                root.textureOverride = jemData.getCustomTexture();
            }
            Map<String, ModelPart> children = new HashMap<>();
            for (Map.Entry<String, EMFModelPartCustom> newPartEntry :
                    newEmfParts.entrySet()) {
                EMFModelPartCustom newPart = newPartEntry.getValue();
                if (thisPartName.equals(newPart.partToBeAttached)) {
                    if (EMF.config().getConfig().logModelCreationData)
                        EMFUtils.log(" > > > EMF custom part attached: " + newPartEntry.getKey());
                    if (!newPart.attach) {
                        thisPart.cuboids = List.of();
                        thisPart.children.values().forEach((part) -> {
                            if (part instanceof EMFModelPartVanilla vanilla && !vanilla.isOptiFinePartSpecified)
                                vanilla.setHideInTheseStates(variant);
                        });
                    }
                    children.put(EMFUtils.getIdUnique(thisPart.children.keySet(), newPartEntry.getKey()), newPart);
                }
            }
            children.putAll(thisPart.vanillaChildren);
            thisPart.children = children;
            thisPart.allKnownStateVariants.put(variant, thisPart.getCurrentState());

        }
        if (!allKnownStateVariants.containsKey(variant))
            allKnownStateVariants.put(variant, EMFModelState.copy(allKnownStateVariants.get(0)));

    }

    public void discoverAndInitVariants() {
        //get random properties and init variants
        String thisDirectoryFileName = variantDirectoryApplier.getThisDirectoryOfFilename(modelName.getNamespace(), modelName.getfileName());
        Identifier propertyID = new Identifier(thisDirectoryFileName + ".properties");
        if (MinecraftClient.getInstance().getResourceManager().getResource(propertyID).isPresent()) {
            variantTester = ETFApi.getVariantSupplierOrNull(propertyID, new Identifier(thisDirectoryFileName + ".jem"), "models");

            if(variantTester instanceof PropertiesRandomProvider propertiesRandomProvider){
                propertiesRandomProvider.setOnMeetsRuleHook((entity,rule)->
                        EMFManager.getInstance().lastModelRuleOfEntity.put(entity.etf$getUuid(),rule.RULE_NUMBER));
            }

            if (variantTester != null) {
                IntOpenHashSet allModelVariants = variantTester.getAllSuffixes();
                allModelVariants.remove(1);
                allModelVariants.remove(0);
                if (!allModelVariants.isEmpty()) {
                    //init all variants
                    for (int variant : allModelVariants) {
                        setVariantStateTo(1);

                        String jemNameVariant = variantDirectoryApplier.getThisDirectoryOfFilename(modelName.getNamespace(), modelName.getfileName() + variant + ".jem");
                        if (EMF.config().getConfig().logModelCreationData)
                            EMFUtils.log(" > incorporating variant jem file: " + jemNameVariant);
                        EMFJemData jemDataVariant = getJemDataWithDirectory(jemNameVariant, modelName);
                        if (jemDataVariant != null) {
                            addVariantOfJem(jemDataVariant, variant);
                            setVariantStateTo(variant);
                            EMFManager.getInstance().setupAnimationsFromJemToModel(jemDataVariant, this, variant);
                            containsCustomModel = true;
                        } else {
                            //make this variant map to 1
                            allKnownStateVariants.put(variant, allKnownStateVariants.get(1));
                            if (EMF.config().getConfig().logModelCreationData)
                                EMFUtils.log(" > invalid jem variant file: " + jemNameVariant);
                        }

                    }
                    //receiveOneTimeRunnable(this::registerModelRunnable);
                } else {
                    if (EMF.config().getConfig().logModelCreationData)
                        EMFUtils.logWarn("properties with only 1 variant found: " + propertyID + ".");
                    //variantTester = null;
                    //variantDirectoryApplier = null;
                }
            } else {
                EMFUtils.logWarn("null properties found for: " + propertyID);
                variantDirectoryApplier = null;
            }
        } else {
            EMFUtils.logWarn("no properties or variants found for found for: " + thisDirectoryFileName + ".jem");
            variantDirectoryApplier = null;
        }
    }


    public void setVariant1ToVanilla0() {
        allKnownStateVariants.put(1, allKnownStateVariants.get(0));
        allVanillaParts.forEach((k, child) -> child.allKnownStateVariants.put(1, child.allKnownStateVariants.get(0)));
    }


    public void tryRenderVanillaRootNormally(MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay) {
        if (vanillaRoot != null) {
            matrixStack.push();
            if (EMF.config().getConfig().vanillaModelHologramRenderMode_2 == EMFConfig.VanillaModelRenderMode.OFFSET) {
                matrixStack.translate(1, 0, 0);
            }
            vanillaRoot.render(matrixStack, vertexConsumer, light, overlay, 1, 0.5f, 0.5f, 0.5f);
            matrixStack.pop();
        }
    }

    public void tryRenderVanillaFormatRoot(MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay) {
        if (EMF.config().getConfig().attemptPhysicsModPatch_2 == EMFConfig.PhysicsModCompatChoice.VANILLA) {
            if (vanillaRoot != null) {
                vanillaRoot.render(matrixStack, vertexConsumer, light, overlay, 1, 1, 1, 1);
            }
        } else {
            ModelPart vanillaFormat = getVanillaFormatRoot();
            if (vanillaFormat != null) {
                vanillaFormat.render(matrixStack, vertexConsumer, light, overlay, 1, 1, 1, 1);
            }
        }
    }

    public ModelPart getVanillaFormatRoot() {
        if (!vanillaFormatModelPartOfEachState.containsKey(currentModelVariant)) {
            vanillaFormatModelPartOfEachState.put(currentModelVariant, getVanillaModelPartsOfCurrentState());
        }
        return vanillaFormatModelPartOfEachState.get(currentModelVariant);
    }

    public void receiveAnimations(int variant, Collection<EMFAnimation> animationList) {// Object2ObjectLinkedOpenHashMap<String, Object2ObjectLinkedOpenHashMap<String, EMFAnimation>> orderedAnimationsByPartName) {
//        LinkedList<EMFAnimation> animationList = new LinkedList<>();
//        if (orderedAnimationsByPartName.size() > 0) {
//            allVanillaParts.values().forEach((emf) -> {
//                if (orderedAnimationsByPartName.containsKey(emf.name)) {
//                    Object2ObjectLinkedOpenHashMap<String, EMFAnimation> anims = orderedAnimationsByPartName.get(emf.name);
//                    if (anims != null && !anims.isEmpty()) {
//                        anims.forEach((key, anim) -> animationList.add(anim));
//                    }
//                }
//            });
//        }
        if (!animationList.isEmpty()) {
            Runnable run = () -> {
                if (lastMobCountAnimatedOn != EMFManager.getInstance().entityRenderCount) {
                    lastMobCountAnimatedOn = EMFManager.getInstance().entityRenderCount;
                    animationList.forEach((EMFAnimation::calculateAndSet));
                }
            };
            allVanillaParts.values().forEach((emf) -> emf.receiveRootAnimationRunnable(variant, run));
        }
    }


    /**
     * Gets top level jem texture.
     * also removes this texture from the child part overrides if this is the first time it's called.
     * This allows optimizing the amount of times the texture is overridden for living entities
     *
     * @return the top level jem texture
     */
    public Identifier getTopLevelJemTexture() {
        if (hasRemovedTopLevelJemTextureFromChildren)
            return textureOverride;

        hasRemovedTopLevelJemTextureFromChildren = true;
        if (textureOverride != null) {
            allVanillaParts.values().forEach((emf) -> {
                if (emf.textureOverride.equals(textureOverride)) emf.textureOverride = null;
            });
        }
        return textureOverride;
    }


    @Override
    public String toString() {
        return "[EMF root part of " + modelName.getfileName() + "]";
    }

    @Override
    public String toStringShort() {
        return "[EMF root part of " + modelName.getfileName() + "]";
    }
}
