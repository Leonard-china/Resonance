"""50 s MIT source / actual AI response / real encrypted companionship film."""
import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont
import produce as base

DURATIONS = [3, 9.5, 11.5, 15.5, 6.5, 6.5]
FADE = .5
TOTAL = sum(DURATIONS) - FADE * (len(DURATIONS) - 1)


def plate(a, scene, vertical):
    w, h = (1080, 1920) if vertical else (1920, 1080)
    yy, xx = np.mgrid[0:h, 0:w]
    glow = np.exp(-((xx / w - .8) ** 2 * 3 + (yy / h - .45) ** 2 * 2))
    im = Image.fromarray((np.array([249, 248, 241]) + glow[..., None] * np.array([-23, -12, -20])).clip(0, 255).astype('uint8'))
    d = ImageDraw.Draw(im)
    ink, green, muted = '#173b3c', '#397c69', '#5f7b76'
    m = 76 if vertical else 108
    def text(x, y, value, size, color=ink, bold=False, anchor=None):
        d.text((x, y), value, font=base.font(a, size, bold), fill=color, anchor=anchor)
    text(m, 50, 'RESONANCE', 27, bold=True)
    text(w-m, 55, '听 见 共 鸣', 20, muted, anchor='ra')
    d.line((m, 106, w-m, 106), fill='#c6d4c9', width=1)
    box = None
    if scene in (0, 5):
        cy = 700 if vertical else 360
        for r in (85, 120, 170, 210):
            d.ellipse((w/2-r, cy-r, w/2+r, cy+r), outline='#bdd2c1', width=2)
        for i, height in enumerate([40, 70, 116, 82, 145, 64, 104, 52, 26]):
            x = w/2 - 104 + i*25
            d.rounded_rectangle((x, cy-height/2, x+10, cy+height/2), radius=5, fill=green)
        text(w/2, cy+245, 'Resonance', 98, bold=True, anchor='mm')
        text(w/2, cy+350, '音乐，因你而共鸣。', 40, muted, anchor='mm')
        if scene == 5:
            text(w/2, cy+445, 'MIT 开源  ·  AI 助手  ·  加密一起听', 28, green, anchor='mm')
            text(w/2, cy+510, 'Android  /  Windows    ·    leonard.asia', 24, muted, anchor='mm')
    else:
        content = {
            1: ('01 / OPEN SOURCE', ['MIT 开源', '让信任可见。'], '自由使用、修改，也欢迎一起完善。', 'github.com/Leonard-china/Resonance'),
            2: ('02 / AI ASSISTANT', ['说出想听的', '让 AI 来帮你。'], '找歌、整理曲库、读懂歌词。', 'DeepSeek 真实回答 · 使用自选模型'),
            3: ('03 / TOGETHER', ['隔着距离', '也能陪着你。'], '端到端加密 · 一起听，也一起聊', '配对一次，下次直接邀听 · 对方接受才加入'),
            4: ('04 / LYRICS', ['每一句', '都在眼前。'], '歌词平滑跟随，随心翻阅。', '原生歌词交互演示 · 原创演示数据'),
        }
        label, titles, desc, detail = content[scene]
        y = 160 if vertical else 260
        text(m, y, label, 24, green)
        for i, title in enumerate(titles): text(m-3, y+65+i*(82 if vertical else 110), title, 68 if vertical else 88, bold=True)
        text(m, y+(250 if vertical else 320), desc, 28, muted)
        text(m, y+(310 if vertical else 380), detail, 21 if vertical else 23, green)
        if scene == 1:
            x, y, sw, sh = (70, 620, 940, 950) if vertical else (1010, 212, 810, 650)
            d.rounded_rectangle((x, y, x+sw, y+sh), radius=25, fill='#102d31')
            cf = ImageFont.truetype(str(a.code_font), 22 if vertical else 19)
            text(x+30, y+28, 'TogetherTransport.kt  /  TLS', 22, '#b8d9c8')
            code = Path(a.source).read_text(encoding='utf-8')
            start = code.index('    fun client(pin: String,')
            lines = code[start:].splitlines()[:23]
            for i, line in enumerate(lines):
                # Repository excerpt, not an invented product screen or executable editor.
                snippet = line.replace('    ', '  ')
                limit = math.floor((sw-60)/cf.getlength('M'))
                if len(snippet)>limit: snippet=snippet[:limit-1]+'…'
                d.text((x+28, y+88+i*(32 if vertical else 22)), snippet, font=cf, fill='#b8e9cc' if 'MessageDigest' in line else '#d8e9df')
            text(x+30, y+sh-55, '实际源码节选 · 证书指纹校验', 22, '#a1c9b3')
        else:
            box = (189, 560, 702, 1248) if vertical else (1240, 142, 459, 816)
            x, y, sw, sh = box
            d.rounded_rectangle((x-14, y-14, x+sw+14, y+sh+14), radius=38, fill='#193033', outline='#728e80', width=2)
    footer = '预览稿 · 源码公开，项目开源许可待确认' if a.license == 'pending' else f'{a.license} 开源 · 原生音乐播放器'
    text(m, h-70, footer, 18, muted)
    text(w-m, h-70, '仓库实际代码节选' if scene == 1 else 'MuMu 实录 · 原创演示音频', 18, muted, anchor='ra')
    return im, box


def native_inserts(a, marks):
    """Edit actual recorded states, retaining original model text and real peer messages."""
    proof = json.loads((a.output / 'promo-ai-proof.json').read_text(encoding='utf-8'))
    if not proof.get('answer') or 'DeepSeek' not in proof.get('model', ''):
        raise ValueError('A captured real AI answer is required')
    cuts = {
        'assistant': [(marks['assistant-input'], 2.5), (marks['assistant-answer'] + .5, 9)],
        'together': [(marks['together'] + .3, 3.5), (marks['paired'], 3.5), (marks['chat'], 8.5)],
    }
    result = {}
    for name, segments in cuts.items():
        args = []
        for start, duration in segments:
            args += ['-ss', max(0, start), '-t', duration, '-i', a.output / 'raw-features.mp4']
        graph = ';'.join(f'[{i}:v]setpts=PTS-STARTPTS,fps=30,setsar=1[v{i}]' for i in range(len(segments)))
        graph += ';' + ''.join(f'[v{i}]' for i in range(len(segments))) + f'concat=n={len(segments)}:v=1:a=0[v]'
        target = a.output / f'insert-{name}.mp4'
        base.run(a.ffmpeg, *args, '-filter_complex', graph, '-map', '[v]', '-an', '-c:v', 'libx264',
                 '-crf', '16', '-preset', 'medium', '-threads', '4', '-pix_fmt', 'yuv420p', target)
        result[name] = target
    return result


def render(a, vertical):
    kind = 'vertical' if vertical else 'landscape'
    work = a.output / kind
    work.mkdir(exist_ok=True)
    marks = base.take_marks(a.output, 'features')
    old = base.take_marks(a.player_output, 'light')
    cuts = {2: (a.output / 'insert-assistant.mp4', 0),
            3: (a.output / 'insert-together.mp4', 0),
            4: (a.player_output / 'raw-light.mp4', old['lyrics']+1.8)}
    clips=[]
    for scene, duration in enumerate(DURATIONS):
        im, box = plate(a, scene, vertical)
        still = work / f'feature-{scene}.png'; im.save(still)
        clip = work / f'feature-{scene}.mp4'; clips.append(clip)
        common=['-c:v','libx264','-preset','medium','-crf','17','-threads','4','-pix_fmt','yuv420p','-an','-t',duration,'-r','30',clip]
        if box is None:
            base.run(a.ffmpeg, '-loop','1','-framerate','30','-i',still,'-vf',
                     f"zoompan=z='1.012-0.012*min(on/{duration*30},1)':x='iw/2-iw/zoom/2':y='ih/2-ih/zoom/2':d=1:s={im.width}x{im.height}:fps=30,setsar=1", *common)
        else:
            x,y,sw,sh=box
            mask=Image.new('L',(sw,sh)); ImageDraw.Draw(mask).rounded_rectangle((0,0,sw-1,sh-1),radius=28,fill=255)
            maskfile=work / 'mask.png'; mask.save(maskfile)
            movie,start=cuts[scene]
            graph=f'[1:v]scale={sw}:{sh}:flags=lanczos,setsar=1,fps=30[s];[s][2:v]alphamerge[r];[0:v][r]overlay={x}:{y}:shortest=1,format=yuv420p[v]'
            base.run(a.ffmpeg,'-loop','1','-framerate','30','-i',still,'-ss',max(0,start),'-i',movie,'-loop','1','-i',maskfile,'-filter_complex',graph,'-map','[v]',*common)
        print(f'Rendered {kind} feature {scene}',flush=True)
    inputs=[v for clip in clips for v in ('-i',clip)]
    graph=[]; previous='0:v'; offset=DURATIONS[0]-FADE
    for i in range(1,len(clips)):
        graph.append(f'[{previous}][{i}:v]xfade=transition=fade:duration={FADE}:offset={offset}[x{i}]')
        previous=f'x{i}'; offset+=DURATIONS[i]-FADE
    graph += [f'[{previous}]fade=t=in:d=0.4,fade=t=out:st={TOTAL-.6}:d=0.6[v]',
              f'[{len(clips)}:a]loudnorm=I=-16:TP=-1.2:LRA=8,afade=t=out:st={TOTAL-2}:d=2[a]']
    final=a.output / f'Resonance-{kind}-features-50s.mp4'
    base.run(a.ffmpeg,*inputs,'-i',a.output/'soundtrack-original.wav','-filter_complex',';'.join(graph),'-map','[v]','-map','[a]',
             '-c:v','libx264','-preset','slow','-crf','17','-threads','4','-pix_fmt','yuv420p','-c:a','aac','-b:a','256k','-t',TOTAL,'-movflags','+faststart',final)


def main():
    p=argparse.ArgumentParser()
    for name in ('ffmpeg','font','bold-font','code-font','source'): p.add_argument('--'+name,required=True)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--player-output',type=Path,required=True)
    p.add_argument('--license',default='MIT',choices=['pending','MIT','Apache-2.0','GPL-3.0'])
    p.add_argument('--prepare-only',action='store_true')
    a=p.parse_args(); a.output.mkdir(parents=True,exist_ok=True)
    base.TOTAL=TOTAL
    base.soundtrack(a.output/'soundtrack-original.wav')
    if a.prepare_only:
        for v in (False,True):
            for scene in (1,2,3): plate(a,scene,v)[0].save(a.output/f'design-{scene}-{"vertical" if v else "landscape"}.png')
        return
    native_inserts(a, base.take_marks(a.output, 'features'))
    for v in (False,True): render(a,v)
    (a.output/'production.json').write_text(json.dumps({'duration':TOTAL,'ai_live_reply':True,'license':a.license,
        'source':'repository excerpt, actual native assistant response, real encrypted chat/pairing, original lyrics UI rig',
        'native_audio':'Windows FFmpeg/Java Sound to MuMu MediaSessionService; separate playback QA logs'},ensure_ascii=False,indent=2),encoding='utf-8')


if __name__ == '__main__': main()
