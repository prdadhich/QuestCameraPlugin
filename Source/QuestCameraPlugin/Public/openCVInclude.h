#pragma once

#include <cstdint>

// Always declare the function so other modules can call it.
// Implementation is in OpenCVWrapper.cpp which includes OpenCV headers.
void ProcessCameraFrame(uint8_t* data, int width, int height);
