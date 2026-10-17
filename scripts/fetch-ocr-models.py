"""Reproduce bundled PP-OCRv5 assets. Development dependency: PyYAML."""
import hashlib
import io
import tarfile
import urllib.request
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'composeApp/src/ocrModels/ocr'
ARCHIVES = {
    'det': '781056046c9ed77a15c94681605db6a0f62317c2e9cce6931c71da2478d4bc30',
    'rec': 'f7e792bc836f36e7ef895ad47c426d75b0b75b1650caa6d63fe9418441ffba8c',
}


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    for kind, expected in ARCHIVES.items():
        url = ('https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/'
               f'paddle3.0.0/PP-OCRv5_mobile_{kind}_onnx_infer.tar')
        with urllib.request.urlopen(url, timeout=120) as response:
            data = response.read(25_000_001)
        if hashlib.sha256(data).hexdigest() != expected:
            raise ValueError(f'{kind}: upstream archive checksum changed; assets not replaced')
        prefix = f'PP-OCRv5_mobile_{kind}_onnx_infer/'
        with tarfile.open(fileobj=io.BytesIO(data)) as archive:
            model = archive.extractfile(prefix + 'inference.onnx').read()
            (DEST / f'{kind}.onnx').write_bytes(model)
            if kind == 'rec':
                config = yaml.safe_load(archive.extractfile(prefix + 'inference.yml'))
                chars = config['PostProcess']['character_dict']
                # CTC index 0 is blank; Paddle's decoder appends an ASCII space.
                (DEST / 'characters.txt').write_text('\n'.join(map(str, chars)) + '\n', encoding='utf-8')
                print(f'CTC dictionary: {len(chars)} characters')
        print(f'{kind}.onnx {len(model)} bytes sha256={hashlib.sha256(model).hexdigest()}')


if __name__ == '__main__':
    main()
