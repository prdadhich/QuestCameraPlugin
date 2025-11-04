using EpicGames.Core;
using System.IO;
using UnrealBuildTool;

public class QuestCameraPlugin : ModuleRules
{
    public QuestCameraPlugin(ReadOnlyTargetRules Target) : base(Target)
    {

        string PluginPath1 = Path.GetFullPath(Path.Combine(ModuleDirectory, "..", ".."));
        string BinariesPath = Path.Combine(PluginPath1, "Binaries", "Android", "arm64-v8a");
        string LibName1 = "libopencv_world.so";
        string FullLibPath = Path.Combine(BinariesPath, LibName1);

        if (Target.Platform == UnrealTargetPlatform.Android)
        {
         
            if (File.Exists(FullLibPath))
            {
                
                PublicAdditionalLibraries.Add(FullLibPath);

               
                RuntimeDependencies.Add(FullLibPath, StagedFileType.NonUFS);
            }
            else
            {
                System.Console.WriteLine("[QuestCameraPlugin] OpenCV .so not found at: " + FullLibPath);
            }




        }





        //Type = ModuleType.External;

        //PCHUsage = PCHUsageMode.NoPCHs;
        //bUseRTTI = true;

        PCHUsage = PCHUsageMode.UseExplicitOrSharedPCHs;

        PublicIncludePaths.Add(Path.Combine(ModuleDirectory, "Public"));
        PrivateIncludePaths.Add(Path.Combine(ModuleDirectory, "Private"));

        PublicDependencyModuleNames.AddRange(new string[]
        {
            "Core",
            "CoreUObject",
            "Engine",
            "ApplicationCore",
            "InputCore",
            "RenderCore",
            "RHI"
        });

        PrivateDependencyModuleNames.AddRange(new string[] { });





        PublicDefinitions.Add("NO_CV_TYPES=1");

        
        string ThirdPartyPath = Path.Combine(ModuleDirectory, "ThirdParty", "OpenCV");
        string IncludePath = Path.Combine(ThirdPartyPath, "include");
        string LibPath = Path.Combine(ThirdPartyPath, "libs", "arm64-v8a");
        string LibName = "libopencv_world.so"; 

       
        if (Directory.Exists(IncludePath))
        {
            PublicIncludePaths.Add(IncludePath);
        }
        else
        {
            System.Console.WriteLine(" OpenCV include path not found: " + IncludePath);
        }

        if (Target.Platform == UnrealTargetPlatform.Android)
        {
            
            if (File.Exists("$(PluginDir)/Binaries/Android/arm64-v8a/libopencv_world.so"))
            {
                PublicAdditionalLibraries.Add("$(PluginDir)/Binaries/Android/arm64-v8a/libopencv_world.so");
                PublicDelayLoadDLLs.Add("libopencv_world.so");


                   RuntimeDependencies.Add(
        "$(PluginDir)/Binaries/Android/arm64-v8a/libopencv_world.so",
        StagedFileType.NonUFS);
                
                RuntimeDependencies.Add(
        "$(PluginDir)/Source/QuestCameraPlugin/ThirdParty/OpenCV/libs/arm64-v8a/libopencv_world.so",
        StagedFileType.NonUFS);



       
            }

            
            string PluginPath = Utils.MakePathRelativeTo(ModuleDirectory, Target.RelativeEnginePath);
            AdditionalPropertiesForReceipt.Add(
                "AndroidPlugin",
                Path.Combine(PluginPath, "QuestCameraPlugin_UPL.xml")
            );

            PublicAdditionalLibraries.Add(PluginPath + "/armv64-v8a/libopencv_world.so");


            PublicDependencyModuleNames.Add("Launch");
            PublicDependencyModuleNames.Add("ImageCore");
        }

        if (Target.Platform == UnrealTargetPlatform.Android)
        {
            PublicDefinitions.Add("CV_DO_NOT_DEFINE_INT64=1");
           


        }



    }
}
