package traben.entity_model_features.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BipedEntityModel.class)
public interface BipedEntityModelAccessor {
    @Invoker
    Iterable<ModelPart> callGetBodyParts();

    @Invoker
    Iterable<ModelPart> callGetHeadParts();
}
