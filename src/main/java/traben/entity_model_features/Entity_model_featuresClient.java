package traben.entity_model_features;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;

@Environment(EnvType.CLIENT)
public class Entity_model_featuresClient implements ClientModInitializer {


    //public static HashMap<String, EntityModel> ENTITYNAME_VanillaModel = new HashMap<>();

    @Override
    public void onInitializeClient() {
        //testing
        LogManager.getLogger().info("[Entity Model Features]: Loading! 1.19.3");
        //init data manager
        EMFData.getInstance();

//        if(FabricLoader.getInstance().isModLoaded("physicsmod")){
//            EMFRagdollHook.addRagdollHook();
//        }
    }

}
