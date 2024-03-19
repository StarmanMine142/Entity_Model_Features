package traben.entity_model_features.utils;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class OptifineMobNameForFileAndEMFMapId {

    private String fileName;
    private String mapId;

    public OptifineMobNameForFileAndEMFMapId(String both) {
        this(both,null);
    }

    private OptifineMobNameForFileAndEMFMapId(String both, String mapId) {
        this.fileName = both;
        this.mapId = mapId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptifineMobNameForFileAndEMFMapId that = (OptifineMobNameForFileAndEMFMapId) o;
        return fileName.equals(that.fileName) && Objects.equals(mapId, that.mapId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, mapId);
    }

    @Override
    public String toString() {
        return fileName;
    }

    public void setBoth(String both) {
        this.fileName = both;
        this.mapId = null;
    }

    public boolean areBothSame() {
        return mapId == null || fileName.equals(mapId);
    }

    public void setBoth(String fileName, String mapId) {
        this.fileName = fileName;
        this.mapId = mapId;
    }

    public String getfileName() {
        return this.fileName;
    }


    public @Nullable OptifineMobNameForFileAndEMFMapId getDeprecated() {
        return deprecatedFileName == null ? null : new OptifineMobNameForFileAndEMFMapId(deprecatedFileName,getMapId());
    }

    private String deprecatedFileName = null;

    public String getNamespace() {
        return namespace;
    }

    private String namespace = "minecraft";
    public void finish(){
        //assert new namespaced modded model folder
        if(getfileName().startsWith("modded/")){
            String[] split = fileName.split("/");
            if (split.length >= 3 && !split[1].isEmpty()) {
                namespace = split[1];
                String modelFileName = fileName.replace("modded/"+namespace+"/","");
                deprecatedFileName = fileName;
                fileName = modelFileName;
            }
        }
    }

    public String getMapId() {
        return mapId == null ? fileName : mapId;
    }


}
