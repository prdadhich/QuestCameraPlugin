#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "CameraTestActor.generated.h"

class UStaticMeshComponent;
class UMaterial;
class UMaterialInstanceDynamic;
class UTexture2D;

UCLASS()
class QUESTCAMERAPLUGIN_API ACameraTestActor : public AActor
{
    GENERATED_BODY()

public:
    ACameraTestActor();

protected:
    virtual void BeginPlay() override;
    virtual void EndPlay(const EEndPlayReason::Type EndPlayReason) override;
    virtual void Tick(float DeltaTime) override;

private:
    /** Display mesh for showing camera feed */
    UPROPERTY(VisibleAnywhere, Category = "Camera")
    UStaticMeshComponent* DisplayPlane;

    /** Base material used for dynamic instance */
    UPROPERTY()
    UMaterial* BaseMaterial;

    /** Dynamic instance of material to display texture */
    UPROPERTY()
    UMaterialInstanceDynamic* DynamicMaterial;

    /** Latest captured texture from Quest camera */
    UPROPERTY()
    UTexture2D* CameraTexture;

    /** Timer for delayed permission and initialization */
    FTimerHandle PermissionDelayTimerHandle;

    /** Initializes Quest camera after permission delay */
    void InitializeCamera();

    /** Updates dynamic texture from camera feed */
    void UpdateCameraTexture();
};
