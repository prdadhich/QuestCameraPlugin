
#include "CameraTestActor.h"
#include "Materials/Material.h"
#include "Materials/MaterialInstanceDynamic.h"
#include "Components/StaticMeshComponent.h"
#include "Engine/Texture2D.h"
#include "UObject/ConstructorHelpers.h"
#include "QuestCameraBlueprintLibrary.h"

ACameraTestActor::ACameraTestActor()
{
    // Set this actor to call Tick() every frame
    PrimaryActorTick.bCanEverTick = true;

    // Create and setup the display plane
    DisplayPlane = CreateDefaultSubobject<UStaticMeshComponent>(TEXT("DisplayPlane"));
    RootComponent = DisplayPlane;

    // Load a plane mesh
    static ConstructorHelpers::FObjectFinder<UStaticMesh> PlaneMeshAsset(TEXT("/Engine/BasicShapes/Plane.Plane"));
    if (PlaneMeshAsset.Succeeded())
    {
        DisplayPlane->SetStaticMesh(PlaneMeshAsset.Object);
    }

    // Load the base material
    static ConstructorHelpers::FObjectFinder<UMaterial> BaseMaterialAsset(TEXT("/Game/M_CameraTexture.M_CameraTexture"));
    if (BaseMaterialAsset.Succeeded())
    {
        BaseMaterial = BaseMaterialAsset.Object; // Store base material
        DisplayPlane->SetMaterial(0, BaseMaterial); // Set base material
    }
    else
    {
        UE_LOG(LogTemp, Warning, TEXT("Failed to load /Game/M_CameraTexture.M_CameraTexture"));
    }

    CameraTexture = nullptr;
    DynamicMaterial = nullptr;
}

void ACameraTestActor::BeginPlay()
{
    Super::BeginPlay();

    // Create dynamic material instance from stored base material
    if (BaseMaterial)
    {
        DynamicMaterial = UMaterialInstanceDynamic::Create(BaseMaterial, this);
        if (DynamicMaterial && DisplayPlane)
        {
            DisplayPlane->SetMaterial(0, DynamicMaterial);
            UE_LOG(LogTemp, Log, TEXT("Dynamic material created and applied"));
        }
        else
        {
            UE_LOG(LogTemp, Error, TEXT("Failed to create dynamic material or apply to DisplayPlane"));
        }
    }
    else
    {
        UE_LOG(LogTemp, Error, TEXT("Base material not loaded"));
    }

    // Request permissions and schedule initialization
    UQuestCameraBlueprintLibrary::RequestPermissions();
    GetWorld()->GetTimerManager().SetTimer(PermissionDelayTimerHandle, this, &ACameraTestActor::InitializeCamera, 2.0f, false);
}

void ACameraTestActor::EndPlay(const EEndPlayReason::Type EndPlayReason)
{
    Super::EndPlay(EndPlayReason);
    UQuestCameraBlueprintLibrary::CloseCamera();
    GetWorld()->GetTimerManager().ClearTimer(PermissionDelayTimerHandle);
}

void ACameraTestActor::Tick(float DeltaTime)
{
    Super::Tick(DeltaTime);
    UpdateCameraTexture();
}

void ACameraTestActor::InitializeCamera()
{
    UQuestCameraBlueprintLibrary::InitCamera();
    UQuestCameraBlueprintLibrary::OpenCamera();
    UE_LOG(LogTemp, Log, TEXT("CameraTestActor: Camera initialized and opened"));
}

void ACameraTestActor::UpdateCameraTexture()
{
    // Call openCamera once, then get latest frame continuously
    static bool bCameraOpened = false;
    if (!bCameraOpened)
    {
        UQuestCameraBlueprintLibrary::OpenCamera();
        bCameraOpened = true;
    }

    UTexture2D* NewTexture = UQuestCameraBlueprintLibrary::CaptureImage();
    if (NewTexture && DynamicMaterial)
    {
        if (CameraTexture != NewTexture)
        {
            CameraTexture = NewTexture;
            DynamicMaterial->SetTextureParameterValue(FName("Texture"), CameraTexture);
            UE_LOG(LogTemp, Log, TEXT("CameraTestActor: Texture updated"));
        }
    }
    else
    {
        UE_LOG(LogTemp, Warning, TEXT("No new texture or dynamic material not set"));
    }

}



