//#include "CoreMinimal.h"
#include "openCVInclude.h"

#ifdef int64
#undef int64
#endif
#ifdef uint64
#undef uint64
#endif

/*
*
#include <string>
#include <string_view>
#include <vector>
#include <memory>
#include <cmath>
#include <type_traits>
#include <cstdint>

//#define CV_DO_NOT_DEFINE_INT64
*/
PRAGMA_PUSH_PLATFORM_DEFAULT_PACKING
THIRD_PARTY_INCLUDES_START
#define CV_DO_NOT_DEFINE_INT64 1
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
THIRD_PARTY_INCLUDES_END
PRAGMA_POP_PLATFORM_DEFAULT_PACKING
//using namespace cv;


void ProcessCameraFrame(uint8_t* data, int width, int height)
{
#if PLATFORM_ANDROID
    if (!data || width <= 0 || height <= 0)
        return;

    cv::Mat img(height, width, CV_8UC4, data);
    cv::Mat gray, edges;
    cv::cvtColor(img, gray, cv::COLOR_RGBA2GRAY);
    cv::Canny(gray, edges, 60, 120);
    img.setTo(cv::Scalar(0, 0, 255, 255), edges);
#endif
}

