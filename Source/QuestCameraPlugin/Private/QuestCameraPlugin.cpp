
// QuestCameraPlugin.cpp
#include "QuestCameraPlugin.h"
#include "Modules/ModuleManager.h"


class FQuestCameraPluginModule : public IModuleInterface
{
public:
    virtual void StartupModule() override {}
    virtual void ShutdownModule() override {}
};


IMPLEMENT_MODULE(FQuestCameraPluginModule, QuestCameraPlugin);

