#pragma once
#include "CoreMinimal.h"
#include "Kismet/BlueprintFunctionLibrary.h"
#include "QuestCameraBlueprintLibrary.generated.h"

UCLASS()
class QUESTCAMERAPLUGIN_API UQuestCameraBlueprintLibrary : public UBlueprintFunctionLibrary
{
    GENERATED_BODY()
public:
    UFUNCTION(BlueprintCallable, Category = "QuestCamera")
    static void InitCamera();

    UFUNCTION(BlueprintCallable, Category = "QuestCamera")
    static void OpenCamera();

    UFUNCTION(BlueprintCallable, Category = "QuestCamera")
    static UTexture2D* CaptureImage();


    UFUNCTION(BlueprintCallable, Category = "QuestCamera")
    static void RequestPermissions();
    UFUNCTION(BlueprintCallable, Category = "QuestCamera")
    static void CloseCamera();
};