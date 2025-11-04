
#include "QuestCameraBlueprintLibrary.h"
#include "Engine/Texture2D.h"
#include "RenderingThread.h"

#include "openCVInclude.h"

#if PLATFORM_ANDROID
#include "Android/AndroidJNI.h"
#include "Android/AndroidApplication.h"

#include <android_native_app_glue.h>
#endif

void UQuestCameraBlueprintLibrary::RequestPermissions()
{
#if PLATFORM_ANDROID
    JNIEnv* Env = FAndroidApplication::GetJavaEnv();
    // Use GameActivity class ID (built-in UE helper)
    jmethodID Method = FJavaWrapper::FindMethod(Env, FJavaWrapper::GameActivityClassID, "requestPermissions", "()V", false);
    FJavaWrapper::CallVoidMethod(Env, FJavaWrapper::GameActivityThis, Method);
    UE_LOG(LogTemp, Log, TEXT("QuestCamera: requestPermissions called"));
#else
    UE_LOG(LogTemp, Warning, TEXT("QuestCamera: RequestPermissions not supported in editor"));
#endif
}

void UQuestCameraBlueprintLibrary::InitCamera()
{
#if PLATFORM_ANDROID
    JNIEnv* Env = FAndroidApplication::GetJavaEnv();
    jmethodID Method = FJavaWrapper::FindMethod(Env, FJavaWrapper::GameActivityClassID, "initCamera", "()V", false);
    FJavaWrapper::CallVoidMethod(Env, FJavaWrapper::GameActivityThis, Method);
    UE_LOG(LogTemp, Log, TEXT("QuestCamera: initCamera called"));
#else
    UE_LOG(LogTemp, Warning, TEXT("QuestCamera: InitCamera not supported in editor"));
#endif
}

void UQuestCameraBlueprintLibrary::OpenCamera()
{
#if PLATFORM_ANDROID
    JNIEnv* Env = FAndroidApplication::GetJavaEnv();
    jmethodID Method = FJavaWrapper::FindMethod(Env, FJavaWrapper::GameActivityClassID, "openCamera", "()V", false);
    FJavaWrapper::CallVoidMethod(Env, FJavaWrapper::GameActivityThis, Method);
    UE_LOG(LogTemp, Log, TEXT("QuestCamera: openCamera called"));
#else
    UE_LOG(LogTemp, Warning, TEXT("QuestCamera: OpenCamera not supported in editor"));
#endif
}

void UQuestCameraBlueprintLibrary::CloseCamera()
{
#if PLATFORM_ANDROID
    JNIEnv* Env = FAndroidApplication::GetJavaEnv();
    jmethodID Method = FJavaWrapper::FindMethod(Env, FJavaWrapper::GameActivityClassID, "closeCamera", "()V", false);
    FJavaWrapper::CallVoidMethod(Env, FJavaWrapper::GameActivityThis, Method);
    UE_LOG(LogTemp, Log, TEXT("QuestCamera: closeCamera called"));
#else
    UE_LOG(LogTemp, Warning, TEXT("QuestCamera: CloseCamera not supported in editor"));
#endif
}

// CaptureImage remains unchanged (it's correct)


UTexture2D* UQuestCameraBlueprintLibrary::CaptureImage()
{
#if PLATFORM_ANDROID
    JNIEnv* Env = FAndroidApplication::GetJavaEnv();
    if (!Env)
    {
        UE_LOG(LogTemp, Error, TEXT("No Java environment!"));
        return nullptr;
    }

    jclass BridgeClass = FAndroidApplication::FindJavaClass("com/stunad/questcamera/PassthroughCameraBridge");
    if (!BridgeClass)
    {
        UE_LOG(LogTemp, Error, TEXT("No PassthroughCameraBridge class!"));
        return nullptr;
    }

    jmethodID GetInstance = FJavaWrapper::FindStaticMethod(Env, BridgeClass, "getInstance", "(Landroid/app/Activity;)Lcom/stunad/questcamera/PassthroughCameraBridge;", false);
    if (!GetInstance)
    {
        UE_LOG(LogTemp, Error, TEXT("getInstance method not found!"));
        return nullptr;
    }

    jobject BridgeInstance = Env->CallStaticObjectMethod(BridgeClass, GetInstance, FAndroidApplication::GetGameActivityThis());
    if (!BridgeInstance)
    {
        UE_LOG(LogTemp, Error, TEXT("No Bridge instance!"));
        return nullptr;
    }

    jmethodID CaptureMethod = FJavaWrapper::FindMethod(Env, BridgeClass, "CaptureImageNow", "()[B", false);
    if (!CaptureMethod)
    {
        UE_LOG(LogTemp, Error, TEXT("CaptureImageNow method not found!"));
        Env->DeleteLocalRef(BridgeInstance);
        return nullptr;
    }

    jbyteArray DataArray = (jbyteArray)Env->CallObjectMethod(BridgeInstance, CaptureMethod);
    if (!DataArray)
    {
        UE_LOG(LogTemp, Warning, TEXT("No frame data returned from CaptureImageNow"));
        Env->DeleteLocalRef(BridgeInstance);
        return nullptr;
    }

    jsize Length = Env->GetArrayLength(DataArray);
    uint8* FrameData = new uint8[Length];
    Env->GetByteArrayRegion(DataArray, 0, Length, reinterpret_cast<jbyte*>(FrameData));



    // Create or update texture (assuming 1280x960 RGBA)
    const int32 Width = 320;
    const int32 Height = 240;

#if PLATFORM_ANDROID
    ProcessCameraFrame(FrameData, Width, Height);
#endif
    if (Length != Width * Height * 4)
    {
        UE_LOG(LogTemp, Error, TEXT("Invalid RGBA data size: expected %d, got %d"), Width * Height * 4, Length);
        delete[] FrameData;
        Env->DeleteLocalRef(BridgeInstance);
        Env->DeleteLocalRef(DataArray);
        return nullptr;
    }

    UTexture2D* NewTexture = UTexture2D::CreateTransient(Width, Height, PF_B8G8R8A8);
    if (!NewTexture)
    {
        UE_LOG(LogTemp, Error, TEXT("Failed to create transient texture!"));
        delete[] FrameData;
        Env->DeleteLocalRef(BridgeInstance);
        Env->DeleteLocalRef(DataArray);
        return nullptr;
    }

    void* TextureData = NewTexture->GetPlatformData()->Mips[0].BulkData.Lock(LOCK_READ_WRITE);
    if (TextureData)
    {
        FMemory::Memcpy(TextureData, FrameData, Length);
        NewTexture->GetPlatformData()->Mips[0].BulkData.Unlock();
        NewTexture->UpdateResource();
    }
    else
    {
        UE_LOG(LogTemp, Error, TEXT("Failed to lock texture data!"));
        NewTexture = nullptr;
    }

    delete[] FrameData;
    Env->DeleteLocalRef(BridgeInstance);
    Env->DeleteLocalRef(DataArray);
    return NewTexture;
#else
    UE_LOG(LogTemp, Warning, TEXT("QuestCamera: CaptureImage not supported in editor"));
    return nullptr;
#endif
}

#if PLATFORM_ANDROID
extern "C" JNIEXPORT void JNICALL
Java_com_stunad_questcamera_PassthroughCameraManager_onFrameAvailable(JNIEnv* env, jclass clazz, jbyteArray data, jint width, jint height)
{
    // Example: Log or process the frame data
    UE_LOG(LogTemp, Log, TEXT("onFrameAvailable called: width=%d, height=%d"), width, height);

    // If needed, convert jbyteArray to uint8* and process
    jsize length = env->GetArrayLength(data);
    uint8* frameData = (uint8*)env->GetByteArrayElements(data, nullptr);
    // Do something with frameData (e.g., update a native buffer)
    env->ReleaseByteArrayElements(data, (jbyte*)frameData, JNI_ABORT);
}

#endif



