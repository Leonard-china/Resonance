# Bundled screenshot OCR

PP-OCRv5 mobile detection and recognition models are distributed by PaddlePaddle
under Apache-2.0. See `ocr/LICENSE-PaddleOCR.txt`. No Python runtime or external
OCR server is required by Resonance. ONNX Runtime（Android 1.23.2 / Windows 1.20.0） is also Apache-2.0.

Original archives (downloaded 2026-10-17):

- https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/paddle3.0.0/PP-OCRv5_mobile_det_onnx_infer.tar
- https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/paddle3.0.0/PP-OCRv5_mobile_rec_onnx_infer.tar

| Packaged asset | SHA-256 |
| --- | --- |
| `ocr/det.onnx` | `a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d` |
| `ocr/rec.onnx` | `da72dc72ca4dc220df0dfde68c1dedc31c58d3e76a25871122e5056227d50092` |
| `ocr/characters.txt` | `1ea29636956177e400af712d9782e7693f3fb25f98617bed10479d2965a836fd` |

Reproduce with Python + PyYAML: `python scripts/fetch-ocr-models.py`. Archive
checksums are pinned in that script. The dictionary comes from the recognition
archive's `PostProcess.character_dict`; the decoder adds CTC blank and space.
Detection uses BGR/ImageNet normalization, recognition uses BGR/0.5 normalization.
The screenshot-only detector uses axis-aligned DB components; rotated photos are
outside the supported input. Model sessions are closed after recognition,
including errors and cancellation; images are decoded one slice at a time.
