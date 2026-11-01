#include <jni.h>
#include <vector>
#include <cstring>
#include "tensorflow/lite/experimental/microfrontend/lib/frontend.h"
#include "tensorflow/lite/experimental/microfrontend/lib/frontend_util.h"

static FrontendState g_state;
static bool g_ready = false;

static constexpr int kFrames = 49;
static constexpr int kBins = 40;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_zpi_slowomir_kws_MicroFrontend_nativeInit(JNIEnv*, jobject) {
    if (g_ready) return JNI_TRUE;
    FrontendConfig cfg;
    FrontendFillConfigWithDefaults(&cfg);
    cfg.window.size_ms = 30;
    cfg.window.step_size_ms = 20;
    cfg.filterbank.num_channels = kBins;
    cfg.filterbank.upper_band_limit = 7500.0f;
    cfg.filterbank.lower_band_limit = 125.0f;
    cfg.noise_reduction.smoothing_bits = 10;
    cfg.noise_reduction.even_smoothing = 0.025f;
    cfg.noise_reduction.odd_smoothing = 0.06f;
    cfg.noise_reduction.min_signal_remaining = 0.05f;
    cfg.pcan_gain_control.enable_pcan = 1;
    cfg.pcan_gain_control.strength = 0.95f;
    cfg.pcan_gain_control.offset = 80.0f;
    cfg.pcan_gain_control.gain_bits = 21;
    cfg.log_scale.enable_log = 1;
    cfg.log_scale.scale_shift = 6;
    g_ready = FrontendPopulateState(&cfg, &g_state, 16000);
    return g_ready ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jfloatArray JNICALL
Java_com_zpi_slowomir_kws_MicroFrontend_nativeProcess(JNIEnv* env, jobject, jshortArray pcm) {
    jfloatArray result = env->NewFloatArray(kFrames * kBins);
    if (!g_ready) return result;

    jsize n = env->GetArrayLength(pcm);
    jshort* data = env->GetShortArrayElements(pcm, nullptr);

    FrontendReset(&g_state);
    std::vector<float> out;
    out.reserve(kFrames * kBins);

    const int16_t* p = reinterpret_cast<const int16_t*>(data);
    size_t remaining = static_cast<size_t>(n);
    while (remaining > 0) {
        size_t read = 0;
        FrontendOutput o = FrontendProcessSamples(&g_state, p, remaining, &read);
        if (read == 0) break;
        p += read;
        remaining -= read;
        for (size_t i = 0; i < o.size; ++i) {
            out.push_back(static_cast<float>(o.values[i]) * (10.0f / 256.0f));
        }
    }
    env->ReleaseShortArrayElements(pcm, data, JNI_ABORT);

    out.resize(kFrames * kBins, 0.0f);
    env->SetFloatArrayRegion(result, 0, kFrames * kBins, out.data());
    return result;
}

JNIEXPORT void JNICALL
Java_com_zpi_slowomir_kws_MicroFrontend_nativeRelease(JNIEnv*, jobject) {
if (g_ready) { FrontendFreeStateContents(&g_state); g_ready = false; }
}

}