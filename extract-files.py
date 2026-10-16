#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/xiaomi/duchamp',
    'hardware/mediatek',
    'hardware/xiaomi'
]

def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}-{partition}' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    ('libtflite_mtk',
     'vendor.mediatek.hardware.apuware.utils-V1-ndk',
     'vendor.mediatek.hardware.apuware.utils@2.0',
     'vendor.mediatek.hardware.camera.isphal-V1-ndk',
     'vendor.mediatek.hardware.videotelephony-V1-ndk'): lib_fixup_vendor_suffix,
}


blob_fixups: blob_fixups_user_type = {
    'vendor/lib64/hw/audio.primary.mediatek.so': blob_fixup()
        .replace_needed('libalsautils.so', 'libalsautils-v34.so')
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),

    'vendor/lib64/hw/android.hardware.soundtrigger3-impl.so': blob_fixup()
        .replace_needed('libaudio_aidl_conversion_common_ndk.so', 'libaudio_aidl_conversion_common_ndk-v34.so'),

    'vendor/lib64/vendor.mediatek.hardware.bluetooth.audio-V1-ndk.so': blob_fixup()
        .replace_needed('android.hardware.audio.common-V1-ndk.so', 'android.hardware.audio.common-V2-ndk.so'),

    ('vendor/etc/init/hw/init.batterysecret.rc',
     'vendor/etc/init/hw/init.mi_thermald.rc'): blob_fixup()
        .regex_replace('.*seclabel.*\n', ''),

    ('vendor/lib64/libarmnn_ndk.mtk.vndk.so',
     'vendor/lib64/mt6897/lib3a.ae.stat.so'): blob_fixup()
        .add_needed('liblog.so'),

    'vendor/lib64/mt6897/lib3a.awb.core.so': blob_fixup()
        .sig_replace('88 81 9d 52 28 00 a0 72 68 6a 68 78 1f 05 00 71 c0 00 00 54 08 84 9d 52 28 00 a0 72 68 6a 68 78 1f 05 00 71 c1 00 00 54 08 ea 87 52 48 00 a0 72 68 6a 68 f8 08 8d 4c f9 e8 0f 00 f9',
                     '88 81 9d 52 28 00 a0 72 68 6a 68 78 1f 05 00 71 08 84 9d 52 28 00 a0 72 68 6a 68 78 04 19 41 7a e1 00 00 54 08 ea 87 52 48 00 a0 72 68 6a 68 f8 68 00 00 b4 08 8d 4c f9 e8 0f 00 f9')
        .sig_replace('68 6a 68 78 1f 05 00 71 e0 00 00 54 0a 84 9d 52 2a 00 a0 72 6a 6a 6a 78 5f 05 00 71 ea 03 1f 2a 21 02 00 54 0a ea 87 52 4a 00 a0 72 6a 6a 6a f8 0b 7c 88 52 4a 69 6b b8',
                     '68 6a 68 78 1f 05 00 71 0a 84 9d 52 2a 00 a0 72 6a 6a 6a 78 44 19 41 7a 41 02 00 54 0a ea 87 52 4a 00 a0 72 6a 6a 6a f8 ca 01 00 b4 0b 7c 88 52 4a 69 6b b8 1f 20 03 d5')
        .sig_replace('69 3a 41 f9 e9 07 00 f9 c0 00 00 54 08 84 9d 52 28 00 a0 72 68 6a 68 78 1f 05 00 71 c1 00 00 54 08 ea 87 52 48 00 a0 72 68 6a 68 f8 08 8d 4c f9 e8 0f 00 f9',
                     '69 3a 41 f9 e9 07 00 f9 08 84 9d 52 28 00 a0 72 68 6a 68 78 04 19 41 7a e1 00 00 54 08 ea 87 52 48 00 a0 72 68 6a 68 f8 68 00 00 b4 08 8d 4c f9 e8 0f 00 f9')
        .sig_replace('08 ea 87 52 48 00 a0 72 13 00 08 8b 7a 02 40 f9 f8 03 00 aa e0 c3 00 91 82 10 80 52 54 53 40 b9 e1 03 1f 2a ff c3 00 b9 ff 02 00 f9',
                     '08 ea 87 52 48 00 a0 72 13 00 08 8b 7a 02 40 f9 5a 17 00 b4 f8 03 00 aa e0 c3 00 91 82 10 80 52 54 53 40 b9 e1 03 1f 2a ff 7e 00 a9')
        .sig_replace('a0 00 00 54 e8 c3 40 f9 08 01 40 79 1f 05 00 71 41 02 00 54 e8 d3 40 f9 00 01 40 f9 08 05 89 52 08 68 68 b8 1f 05 00 71 81 01 00 54',
                     'e8 c3 40 f9 08 01 40 79 04 19 41 7a 61 02 00 54 e8 d3 40 f9 00 01 40 f9 00 02 00 b4 08 05 89 52 08 68 68 b8 1f 05 00 71 81 01 00 54'),

    'vendor/lib64/mt6897/libmtkcam_hal_aidl_common.so': blob_fixup()
        .replace_needed('android.hardware.camera.common-V2-ndk.so', 'android.hardware.camera.common-V1-ndk.so'),

    ('vendor/lib64/mt6897/libmtkcam_grallocutils.so',
     'vendor/lib64/libmtkcam_grallocutils_aidlv1helper.so'): blob_fixup()
        .replace_needed('android.hardware.graphics.allocator-V1-ndk.so', 'android.hardware.graphics.allocator-V2-ndk.so')
        .replace_needed('android.hardware.graphics.common-V4-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),

    ('odm/lib64/libTrueSight.so',
     'odm/lib64/libalLDC.so',
     'odm/lib64/libalAILDC.so',
     'odm/lib64/libalhLDC.so',
     'vendor/lib64/libMiPhotoFilter.so',
     'vendor/lib64/mt6897/libneuralnetworks_sl_driver_mtk_prebuilt.so'): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_createFromHandle')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_getNativeHandle')
        .clear_symbol_version('AHardwareBuffer_isSupported')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),

    'vendor/lib64/mt6897/libmtkcam_hwnode.jpegnode.so': blob_fixup()
        .replace_needed('libultrahdr.so', 'libultrahdr-v34.so'),

    'vendor/lib64/libultrahdr-v34.so': blob_fixup()
        .replace_needed('libjpegencoder.so', 'libjpegencoder-v34.so')
        .replace_needed('libjpegdecoder.so', 'libjpegdecoder-v34.so'),

    ('vendor/bin/hw/mt6897/android.hardware.graphics.allocator-V2-service-mediatek.mt6897',
     'vendor/lib64/egl/mt6897/libGLES_mali.so',
     'vendor/lib64/hw/mt6897/android.hardware.graphics.allocator-V2-mediatek.so',
     'vendor/lib64/hw/mt6897/android.hardware.graphics.mapper@4.0-impl-mediatek.so',
     'vendor/lib64/hw/mt6897/mapper.mediatek.so',
     'vendor/lib64/libaimemc.so',
     'vendor/lib64/vendor.mediatek.hardware.camera.isphal-V1-ndk.so',
     'vendor/lib64/vendor.mediatek.hardware.pq_aidl-V2-ndk.so',
     'vendor/lib64/vendor.mediatek.hardware.pq_aidl-V4-ndk.so',
     'vendor/lib64/vendor.mediatek.hardware.pq_aidl-V6-ndk.so'): blob_fixup()
        .replace_needed('android.hardware.graphics.common-V4-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),

    'vendor/lib64/mt6897/libpqconfig.so': blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so'),

    ('vendor/etc/init/android.hardware.graphics.allocator-V2-service-mediatek.rc',
     'vendor/etc/init/android.hardware.graphics.composer@3.2-service.rc' ): blob_fixup()
        .regex_replace('.*writepid.*\n', ''),

    ('odm/lib64/hw/displayfeature.default.so',
     'vendor/lib64/hw/mt6897/vendor.mediatek.hardware.pq_aidl-impl.so',
     'vendor/lib64/libaudiocloudctrl.so',
     'vendor/lib64/libpqxmlflagparser.so',
     'vendor/lib64/libpqxmlparser.so',
     'vendor/lib64/librt_extamp_intf.so',
     'vendor/lib64/libsilkybrightnesscore.so',
     'vendor/lib64/mt6897/libmmlpqImpl.so'): blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),

    'vendor/bin/hw/android.hardware.security.keymint@3.0-service.mitee': blob_fixup()
        .replace_needed('lib_android_keymaster_keymint_utils.so', 'lib_android_keymaster_keymint_utils_V3.so')
        .replace_needed('libkeymint.so', 'libkeymint_V3.so'),

    'vendor/lib64/libkeymint_V3.so': blob_fixup()
        .replace_needed('lib_android_keymaster_keymint_utils.so', 'lib_android_keymaster_keymint_utils_V3.so'),

    'vendor/lib64/libkeymint_remote_prov_support_V3.so': blob_fixup()
        .replace_needed('libbase.so', 'libbase-v34.so'),

    'odm/lib64/libmt_mitee.so': blob_fixup()
        .replace_needed('lib_android_keymaster_keymint_utils.so', 'lib_android_keymaster_keymint_utils_V3.so')
        .replace_needed('libkeymint.so', 'libkeymint_V3.so')
        .replace_needed('libkeymint_remote_prov_support.so', 'libkeymint_remote_prov_support_V3.so')
        .replace_needed('libkeymint_support.so', 'libkeymint_support_V3.so'),

    'vendor/bin/hw/android.hardware.media.c2@1.2-mediatek-64b': blob_fixup()
        .replace_needed('libcodec2_hidl@1.0.so', 'libcodec2_hidl@1.0-v34.so')
        .replace_needed('libcodec2_hidl@1.1.so', 'libcodec2_hidl@1.1-v34.so')
        .replace_needed('libcodec2_hidl@1.2.so', 'libcodec2_hidl@1.2-v34.so')
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so'),

    'vendor/etc/vintf/manifest/manifest_media_c2_V1_2_default.xml': blob_fixup()
        .regex_replace('.+dolby.+\n', ''),

    'vendor/lib64/libcodec2_fsr.so': blob_fixup()
        .replace_needed('android.hardware.graphics.common-V4-ndk.so', 'android.hardware.graphics.common-V7-ndk.so')
        .replace_needed('libui.so', 'libui-v34.so'),

    'vendor/lib64/libcodec2_hidl@1.0-v34.so': blob_fixup()
        .replace_needed('libstagefright_bufferqueue_helper.so', 'libstagefright_bufferqueue_helper-v36.so')
        .replace_needed('libcodec2_hidl_plugin.so', 'libcodec2_hidl_plugin-v34.so')
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so')
        .replace_needed('libui.so', 'libui-v34.so'),

    'vendor/lib64/libcodec2_hidl@1.1-v34.so': blob_fixup()
        .replace_needed('libstagefright_bufferqueue_helper.so', 'libstagefright_bufferqueue_helper-v36.so')
        .replace_needed('libcodec2_hidl@1.0.so', 'libcodec2_hidl@1.0-v34.so')
        .replace_needed('libcodec2_hidl_plugin.so', 'libcodec2_hidl_plugin-v34.so')
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so')
        .replace_needed('libui.so', 'libui-v34.so'),

    'vendor/lib64/libcodec2_hidl@1.2-v34.so': blob_fixup()
        .replace_needed('libstagefright_bufferqueue_helper.so', 'libstagefright_bufferqueue_helper-v36.so')
        .replace_needed('libcodec2_hidl@1.0.so', 'libcodec2_hidl@1.0-v34.so')
        .replace_needed('libcodec2_hidl@1.1.so', 'libcodec2_hidl@1.1-v34.so')
        .replace_needed('libcodec2_hidl_plugin.so', 'libcodec2_hidl_plugin-v34.so')
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so')
        .replace_needed('libui.so', 'libui-v34.so'),

    ('vendor/lib64/libcodec2_hidl_plugin-v34.so',
     'vendor/lib64/libsfplugin_ccodec_utils-v34.so'): blob_fixup()
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so'),

    ('vendor/lib64/libcodec2_mtk_c2store.so',
     'vendor/lib64/libcodec2_mtk_vdec.so',
     'vendor/lib64/libcodec2_mtk_venc.so',
     'vendor/lib64/libcodec2_vpp_fa_plugin.so',
     'vendor/lib64/libcodec2_vpp_mi_plugin.so',
     'vendor/lib64/libcodec2_vpp_qt_plugin.so',
     'vendor/lib64/libcodec2_vpp_rs_plugin.so'): blob_fixup()
        .replace_needed('libcodec2_soft_common.so', 'libcodec2_soft_common-v34.so')
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so')
        .replace_needed('libsfplugin_ccodec_utils.so', 'libsfplugin_ccodec_utils-v34.so')
        .replace_needed('libui.so', 'libui-v34.so'),

    'vendor/lib64/libcodec2_soft_common-v34.so': blob_fixup()
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so')
        .replace_needed('libsfplugin_ccodec_utils.so', 'libsfplugin_ccodec_utils-v34.so')
        .replace_needed('libui.so', 'libui-v34.so'),

    ('vendor/lib64/libcodec2_vpp_AIMEMC_plugin.so',
     'vendor/lib64/libcodec2_vpp_AISR_plugin.so'): blob_fixup()
        .replace_needed('android.hardware.graphics.common-V4-ndk.so', 'android.hardware.graphics.common-V7-ndk.so')
        .replace_needed('libcodec2_soft_common.so', 'libcodec2_soft_common-v34.so')
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk-v34.so')
        .replace_needed('libsfplugin_ccodec_utils.so', 'libsfplugin_ccodec_utils-v34.so'),

    'vendor/lib64/libcodec2_vndk-v34.so': blob_fixup()
        .replace_needed('libui.so', 'libui-v34.so')
        .replace_needed('android.hardware.media.bufferpool2-V1-ndk.so', 'android.hardware.media.bufferpool2-V2-ndk.so'),

    'system_ext/priv-app/ImsService/ImsService.apk': blob_fixup()
        .apktool_patch('blob-patches/ImsService'),

    'system_ext/lib64/libimsma.so': blob_fixup()
        .replace_needed('libsink.so', 'libsink-mtk.so'),

    'odm/bin/hw/vendor.xiaomi.sensor.citsensorservice.aidl': blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so')
        .replace_needed('libui.so', 'libui-v34.so'),


    'system_ext/lib64/vendor.mediatek.hardware.camera.isphal-V1-ndk.so': blob_fixup()
        .replace_needed('android.hardware.graphics.common-V5-ndk.so', 'android.hardware.graphics.common-V7-ndk.so')
        .replace_needed('android.hardware.graphics.common-V6-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),

    ('system_ext/lib64/libcamera_algoup_jni.xiaomi.so',
     'system_ext/lib64/libcamera_mianode_jni.xiaomi.so',
     'system_ext/lib64/libcamera_ispinterface_jni.xiaomi.so'): blob_fixup()
        .add_needed('libgui_shim_miuicamera.so'),

     'system_ext/priv-app/MiuiCamera/MiuiCamera.apk': blob_fixup()
        .apktool_patch('blob-patches/MIUICamera/'),
     'vendor/etc/init/tee-supplicant.rc': blob_fixup()
        .regex_replace('chown system system /dev/ufs-bsg0', 'chown root system /dev/ufs-bsg0'),
}  # fmt: skip

module = ExtractUtilsModule(
    'duchamp',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    add_firmware_proprietary_file=True,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
