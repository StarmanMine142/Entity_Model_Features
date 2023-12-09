package traben.entity_model_features.models;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import traben.entity_model_features.models.animation.EMFAnimationHelper;
import traben.entity_model_features.utils.EMFManager;
import traben.entity_texture_features.ETFClientCommon;
import traben.entity_texture_features.features.ETFManager;
import traben.entity_texture_features.features.ETFRenderContext;

import java.util.*;

import static traben.entity_model_features.EMFClient.EYES_FEATURE_LIGHT_VALUE;

public abstract class EMFModelPart extends ModelPart {
    public Identifier textureOverride;
//    protected BufferBuilder MODIFIED_RENDER_BUFFER = null;
    private long lastTextureOverride = -1L;


    public EMFModelPart(List<Cuboid> cuboids, Map<String, ModelPart> children) {
        super(cuboids, children);

        // re assert children and cuboids as modifiable
        // required for sodium post 0.5.4
        // this should not cause issues as emf does not allow these model parts to pass through sodium's unique renderer
        this.cuboids = new ObjectArrayList<>(cuboids);
        this.children = new Object2ObjectOpenHashMap<>(children);
    }



    void renderWithTextureOverride(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {

        if (textureOverride == null
                || lastTextureOverride == EMFManager.getInstance().entityRenderCount) {//prevents texture overrides carrying over into feature renderers that reuse the base model
            //normal vertex consumer
            renderLikeETF(matrices, vertices, light, overlay, red, green, blue, alpha);
        } else if (light != EYES_FEATURE_LIGHT_VALUE // this is only the case for EyesFeatureRenderer
                && !ETFRenderContext.isIsInSpecialRenderOverlayPhase() && ETFRenderContext.getCurrentProvider() != null) { //do not allow new etf emissive rendering here

            lastTextureOverride = EMFManager.getInstance().entityRenderCount;

            RenderLayer originalLayer = ETFRenderContext.getCurrentRenderLayer();
            RenderLayer layerModified = EMFAnimationHelper.getLayerFromRecentFactoryOrTranslucent(textureOverride);
            VertexConsumer newConsumer = ETFRenderContext.processVertexConsumer(ETFRenderContext.getCurrentProvider(), layerModified);

            renderLikeVanilla(matrices, newConsumer, light, overlay, red, green, blue, alpha);

            ETFRenderContext.startSpecialRenderOverlayPhase();
            emf$renderEmissive(matrices, overlay, red, green, blue, alpha);
            emf$renderEnchanted(matrices, light, overlay, red, green, blue, alpha);
            ETFRenderContext.endSpecialRenderOverlayPhase();

            //reset render settings
            ETFRenderContext.processVertexConsumer(ETFRenderContext.getCurrentProvider(), originalLayer);
        }
        //else cancel out render
    }

    //required for sodium 0.5.4+
    void renderLikeVanilla(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        if (this.visible) {
            if (!cuboids.isEmpty() || !children.isEmpty()) {
                matrices.push();
                this.rotate(matrices);
                if (!this.hidden) {
                    this.renderCuboids(matrices.peek(), vertices, light, overlay, red, green, blue, alpha);
                }

                for (ModelPart modelPart : children.values()) {
                    modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha);
                }

                matrices.pop();
            }
        }
    }

    //mimics etf model part mixins which can no longer be relied on due to sodium 0.5.4
    void renderLikeETF(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        //etf ModelPartMixin copy
        ETFRenderContext.incrementCurrentModelPartDepth();

        renderLikeVanilla(matrices, vertices, light, overlay, red, green, blue, alpha);

        //etf ModelPartMixin copy
        if (ETFRenderContext.getCurrentModelPartDepth() != 1) {
            ETFRenderContext.decrementCurrentModelPartDepth();
        } else {
            if (ETFRenderContext.isRenderReady()) {
                //attempt special renders as eager OR checks
                if (etf$renderEmissive(matrices, overlay, red, green, blue, alpha) |
                        etf$renderEnchanted(matrices, light, overlay, red, green, blue, alpha)) {
                    //reset render layer stuff behind the scenes if special renders occurred
                    ETFRenderContext.getCurrentProvider().getBuffer(ETFRenderContext.getCurrentRenderLayer());
                }
            }
            //ensure model count is reset
            ETFRenderContext.resetCurrentModelPartDepth();
        }
    }

    public void renderBoxes(MatrixStack matrices, VertexConsumer vertices){
        if (this.visible) {
            if (!cuboids.isEmpty() || !children.isEmpty()) {
                matrices.push();
                this.rotate(matrices);
                if (!this.hidden) {
                    for (Cuboid cuboid : cuboids) {
                        Box box = new Box(cuboid.minX / 16, cuboid.minY / 16, cuboid.minZ / 16, cuboid.maxX / 16, cuboid.maxY / 16, cuboid.maxZ / 16);
                        WorldRenderer.drawBox(matrices, vertices, box, 1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
                for (ModelPart modelPart : children.values()) {
                    if(modelPart instanceof EMFModelPart emf)
                        emf.renderBoxes(matrices, vertices);
                }
                matrices.pop();
            }
        }
    }

    //required for sodium pre 0.5.4
    @Override
    // overrides to circumvent sodium optimizations that mess with custom uv quad creation and swapping out cuboids
    protected void renderCuboids(MatrixStack.Entry entry, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        //this is a copy of the vanilla renderCuboids() method
        for (Cuboid cuboid : cuboids) {
            cuboid.renderCuboid(entry, vertexConsumer, light, overlay, red, green, blue, alpha);
        }
    }

    public String simplePrintChildren(int depth){
        StringBuilder mapper = new StringBuilder();
        mapper.append("\n  | ");
        mapper.append("- ".repeat(Math.max(0, depth)));
        mapper.append(this.toStringShort());
        for (ModelPart child:
             children.values()) {
            if(child instanceof EMFModelPart emf){
                mapper.append(emf.simplePrintChildren(depth+1));
            }
        }
        return mapper.toString();
    }

    public String toStringShort() {
        return toString();
    }

    @Override
    public String toString() {
        return "generic emf part";
    }



//    private static int indent = 0;
    public ModelPart getVanillaModelPartsOfCurrentState(){
//        indent++;
        Map<String, ModelPart> children = new HashMap<>();
        for (Map.Entry<String, ModelPart> child :
                this.children.entrySet()) {
            if (child.getValue() instanceof EMFModelPart emf) {
                children.put(child.getKey(), emf.getVanillaModelPartsOfCurrentState());
            }
        }
//        indent--;
//        for (int i = 0; i < indent; i++) {
//            System.out.print("\\ ");
//        }
//        System.out.print("made: "+ this + "\n");
        List<Cuboid> cubes;
        if(cuboids.isEmpty()){
            cubes = List.of(new Cuboid(0,0,0,0,0,0,0,0,0,0,0,false,0,0, Set.of()));
        }else{
            cubes = cuboids;
        }

        ModelPart part = new ModelPart(cubes, children);
        part.setDefaultTransform(getDefaultTransform());
        part.pitch = pitch;
        part.roll = roll;
        part.yaw = yaw;
        part.pivotZ = pivotZ;
        part.pivotY = pivotY;
        part.pivotX = pivotX;
        part.xScale = xScale;
        part.yScale = yScale;
        part.zScale = zScale;

        return part;
    }

    public Object2ReferenceOpenHashMap<String, EMFModelPart> getAllChildPartsAsAnimationMap(String prefixableParents, int variantNum, Map<String, String> optifinePartNameMap) {
        if (this instanceof EMFModelPartRoot root)
            root.setVariantStateTo(variantNum);

        Object2ReferenceOpenHashMap<String, EMFModelPart> mapOfAll = new Object2ReferenceOpenHashMap<>();
        //Map<String, ModelPart> children = this.children;

        for (ModelPart part :
                children.values()) {
            if (part instanceof EMFModelPart part3) {
                String thisKey;
                boolean addThis;
                if (part instanceof EMFModelPartCustom partc) {
                    thisKey = partc.id;
                    addThis = true;
                } else if (part instanceof EMFModelPartVanilla partv) {
                    thisKey = partv.name;
                    addThis = partv.isOptiFinePartSpecified;
                } else {
                    thisKey = "NULL_KEY_NAME";
                    addThis = false;
                }
                for (Map.Entry<String, String> entry :
                        optifinePartNameMap.entrySet()) {
                    if (entry.getValue().equals(thisKey)) {
                        thisKey = entry.getKey();
                        break;
                    }
                }
                if (addThis) {
                    mapOfAll.put(thisKey, part3);
                    if (prefixableParents.isBlank()) {
                        mapOfAll.putAll(part3.getAllChildPartsAsAnimationMap(thisKey, variantNum, optifinePartNameMap));
                    } else {
                        mapOfAll.put(prefixableParents + ':' + thisKey, part3);
                        mapOfAll.putAll(part3.getAllChildPartsAsAnimationMap(prefixableParents + ':' + thisKey, variantNum, optifinePartNameMap));
                    }
                } else {
                    mapOfAll.putAll(part3.getAllChildPartsAsAnimationMap(prefixableParents, variantNum, optifinePartNameMap));
                }

            }

        }
        return mapOfAll;
    }

    //copy of etf rewrite emissive rendering code
    private void emf$renderEmissive(MatrixStack matrices, int overlay, float red, float green, float blue, float alpha) {
        Identifier emissive = ETFRenderContext.getCurrentETFTexture().getEmissiveIdentifierOfCurrentState();
        if (emissive != null) {

            boolean wasAllowed = ETFRenderContext.isAllowedToRenderLayerTextureModify();
            ETFRenderContext.preventRenderLayerTextureModify();

            boolean textureIsAllowedBrightRender = ETFManager.getEmissiveMode() == ETFManager.EmissiveRenderModes.BRIGHT
                    && ETFRenderContext.getCurrentEntity().etf$canBeBright();// && !ETFRenderContext.getCurrentETFTexture().isPatched_CurrentlyOnlyArmor();

            VertexConsumer emissiveConsumer = ETFRenderContext.getCurrentProvider().getBuffer(
                    textureIsAllowedBrightRender ?
                            RenderLayer.getBeaconBeam(emissive, true) :
                            ETFRenderContext.getCurrentEntity().etf$isBlockEntity() ?
                                    RenderLayer.getEntityTranslucentCull(emissive) :
                                    RenderLayer.getEntityTranslucent(emissive));


            if (wasAllowed) ETFRenderContext.allowRenderLayerTextureModify();

            renderLikeVanilla(matrices, emissiveConsumer, ETFClientCommon.EMISSIVE_FEATURE_LIGHT_VALUE, overlay, red, green, blue, alpha);

        }
    }
    private boolean etf$renderEmissive(MatrixStack matrices, int overlay, float red, float green, float blue, float alpha) {
        Identifier emissive = ETFRenderContext.getCurrentETFTexture().getEmissiveIdentifierOfCurrentState();
        if (emissive != null) {
            boolean wasAllowed = ETFRenderContext.isAllowedToRenderLayerTextureModify();
            ETFRenderContext.preventRenderLayerTextureModify();

            boolean textureIsAllowedBrightRender = ETFManager.getEmissiveMode() == ETFManager.EmissiveRenderModes.BRIGHT
                    && ETFRenderContext.getCurrentEntity().etf$canBeBright();// && !ETFRenderContext.getCurrentETFTexture().isPatched_CurrentlyOnlyArmor();

            VertexConsumer emissiveConsumer = ETFRenderContext.getCurrentProvider().getBuffer(
                    textureIsAllowedBrightRender ?
                            RenderLayer.getBeaconBeam(emissive, true) :
                            ETFRenderContext.getCurrentEntity().etf$isBlockEntity() ?
                                    RenderLayer.getEntityTranslucentCull(emissive) :
                                    RenderLayer.getEntityTranslucent(emissive));

            if(wasAllowed) ETFRenderContext.allowRenderLayerTextureModify();

            ETFRenderContext.startSpecialRenderOverlayPhase();
            renderLikeVanilla(matrices, emissiveConsumer, ETFClientCommon.EMISSIVE_FEATURE_LIGHT_VALUE, overlay, red, green, blue, alpha);
            ETFRenderContext.endSpecialRenderOverlayPhase();
            return true;
        }
        return false;
    }

    //copy of etf enchanted render code
    private void emf$renderEnchanted(MatrixStack matrices, int light, int overlay, float red, float green, float blue, float alpha) {
        //attempt enchanted render
        Identifier enchanted = ETFRenderContext.getCurrentETFTexture().getEnchantIdentifierOfCurrentState();
        if (enchanted != null) {
            boolean wasAllowed = ETFRenderContext.isAllowedToRenderLayerTextureModify();
            ETFRenderContext.preventRenderLayerTextureModify();
            VertexConsumer enchantedVertex = ItemRenderer.getArmorGlintConsumer(ETFRenderContext.getCurrentProvider(), RenderLayer.getArmorCutoutNoCull(enchanted), false, true);
            if (wasAllowed) ETFRenderContext.allowRenderLayerTextureModify();

            renderLikeVanilla(matrices, enchantedVertex, light, overlay, red, green, blue, alpha);
        }
    }
    private boolean etf$renderEnchanted(MatrixStack matrices, int light, int overlay, float red, float green, float blue, float alpha) {
        //attempt enchanted render
        Identifier enchanted = ETFRenderContext.getCurrentETFTexture().getEnchantIdentifierOfCurrentState();
        if (enchanted != null) {
            boolean wasAllowed = ETFRenderContext.isAllowedToRenderLayerTextureModify();
            ETFRenderContext.preventRenderLayerTextureModify();
            VertexConsumer enchantedVertex = ItemRenderer.getArmorGlintConsumer(ETFRenderContext.getCurrentProvider(), RenderLayer.getArmorCutoutNoCull(enchanted), false, true);
            if(wasAllowed) ETFRenderContext.allowRenderLayerTextureModify();

            ETFRenderContext.startSpecialRenderOverlayPhase();
            renderLikeVanilla(matrices, enchantedVertex, light, overlay, red, green, blue, alpha);
            ETFRenderContext.endSpecialRenderOverlayPhase();
            return true;
        }
        return false;
    }


    public static class Animator implements Runnable {
        private Runnable animation = null;

        Animator() {

        }

        public Runnable getAnimation() {
            return animation;
        }

        public void setAnimation(Runnable animation) {
            this.animation = animation;
        }

        public void run() {
            if (animation != null) animation.run();
        }
    }
}
