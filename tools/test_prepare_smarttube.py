import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("prepare_smarttube", Path(__file__).with_name("prepare_smarttube.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class RelocationTests(unittest.TestCase):
    def test_reflection_xml_descriptors_and_jni_names_are_relocated_together(self):
        text = '''package com.google.android.exoplayer2;
Class.forName("com.google.android.exoplayer2.SimpleExoPlayer");
<com.google.android.exoplayer2.ui.PlayerView />
Lcom/google/android/exoplayer2/Player;
Java_com_google_android_exoplayer2_ext_vp9_VpxLibrary_init
'''
        converted = module.relocate(text)
        for old, new in module.REPLACEMENTS:
            self.assertNotIn(old, converted)
            self.assertIn(new, converted)
        self.assertEqual(converted, module.relocate(converted))

    def test_proto_codegen_package_and_host_compiler_override(self):
        proto = 'option java_package = "com.google.android.exoplayer2.source.sabr.protos";'
        self.assertIn('dev.oneyoutubeplayer.smarttube.exoplayer2.source.sabr.protos',
                      module.transform_source('library/sabr/src/main/proto/test.proto', proto))
        self.assertEqual("protobufVersion = '3.25.5'",
                         module.transform_source('exoplayer-amzn-2.10.6/constants.gradle',
                                                 "protobufVersion = '3.17.3'"))


if __name__ == "__main__":
    unittest.main()
