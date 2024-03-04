package traben.entity_model_features.mixin;


import com.google.common.collect.ImmutableMap;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.entity_model_features.EMFClient;
import traben.entity_model_features.config.EMFConfig;
import traben.entity_model_features.utils.EMFManager;
import traben.entity_model_features.utils.EMFUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(BlockEntityRendererFactories.class)
public class MixinBlockEntityRendererFactories {

    @Unique
    private static final List<String> emf$renderers = new ArrayList<>();

    @Inject(method = "reload", at = @At(value = "RETURN"))
    private static void emf$clearMarker(final BlockEntityRendererFactory.Context args, final CallbackInfoReturnable<Map<BlockEntityType<?>, BlockEntityRenderer<?>>> cir) {
        EMFManager.getInstance().currentSpecifiedModelLoading = "";
        EMFManager.getInstance().currentBlockEntityTypeLoading = null;
        if (EMFConfig.getConfig().logModelCreationData || EMFConfig.getConfig().modelExportMode != EMFConfig.ModelPrintMode.NONE)
            EMFUtils.log("Identified block entity renderers: " + emf$renderers);
        emf$renderers.clear();
    }

    @Inject(method = "method_32145", at = @At(value = "HEAD"))
    private static void setEmf$Model(ImmutableMap.Builder<?, ?> builder, BlockEntityRendererFactory.Context context, BlockEntityType<?> type, BlockEntityRendererFactory<?> factory, CallbackInfo ci) {
        //mark which variant is currently specified for use by otherwise identical block entity renderers
        if (EMFClient.testForForgeLoadingError()) return;

        EMFManager.getInstance().currentBlockEntityTypeLoading = type;

        if (BlockEntityType.ENCHANTING_TABLE.equals(type))
            EMFManager.getInstance().currentSpecifiedModelLoading = "enchanting_book";
        else if (BlockEntityType.LECTERN.equals(type))
            EMFManager.getInstance().currentSpecifiedModelLoading = "lectern_book";
        else if (Registries.BLOCK_ENTITY_TYPE.getKey(type).isPresent()) {
            Identifier id = Registries.BLOCK_ENTITY_TYPE.getKey(type).get().getValue();
            if (id.getNamespace().equals("minecraft")) {
                EMFManager.getInstance().currentSpecifiedModelLoading = id.getPath();
            } else {
                EMFManager.getInstance().currentSpecifiedModelLoading = "modded/" + id.getNamespace() + "/" + id.getPath();
            }
        }

        emf$renderers.add(EMFManager.getInstance().currentSpecifiedModelLoading);
    }
}
