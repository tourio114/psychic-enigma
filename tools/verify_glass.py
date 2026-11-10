from pathlib import Path
import moderngl,struct,math
from PIL import Image
root=Path(__file__).resolve().parents[1]
c=moderngl.create_standalone_context(backend='egl',require=330)
vert=(root/'src/main/resources/assets/dynamicisland/shaders/core/liquid_glass.vsh').read_text()
frag=(root/'src/main/resources/assets/dynamicisland/shaders/core/liquid_glass.fsh').read_text()
W,H=320,180
raw=bytes(v for y in range(H) for x in range(W) for v in ((235,155,65,255) if (x//3+y//3)%2 else (35,105,215,255)))
scene=c.texture((W,H),4,raw);scene.filter=(moderngl.LINEAR,moderngl.LINEAR)
identity=struct.pack('16f',1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1)
# UBO std140 size: 2 matrices, vec4, padded vec3.
dynamic=c.buffer(identity+identity+struct.pack('8f',1,1,1,1,0,0,0,0));projection=c.buffer(identity)
def render(source,alpha):
 p=c.program(vertex_shader=vert,fragment_shader=source)
 p['DynamicTransforms'].binding=0;p['Projection'].binding=1
 dynamic.bind_to_uniform_block(0);projection.bind_to_uniform_block(1)
 p['Sampler0'].value=0;scene.use(0)
 x0,y0,x1,y1=32,44,288,136
 packed=[]
 for x,y,u,v in [(x0,y0,0,0),(x0,y1,0,1),(x1,y0,1,0),(x1,y1,1,1)]:
  packed.extend([x/W*2-1,y/H*2-1,0,u,v,14/(y1-y0),.5,.5,alpha])
 buffer=c.buffer(struct.pack(f'{len(packed)}f',*packed))
 vao=c.vertex_array(p,[(buffer,'3f 2f 4f','Position','UV0','Color')])
 out=c.texture((W,H),4);fbo=c.framebuffer([out]);fbo.use();fbo.clear();c.viewport=(0,0,W,H)
 vao.render(moderngl.TRIANGLE_STRIP)
 image=Image.frombytes('RGBA',(W,H),fbo.read(components=4))
 vao.release();buffer.release();p.release();fbo.release();out.release();return image
img=render(frag,.62);reference=render(frag.replace('vec2 blur = texel * 2.6;', 'vec2 blur = vec2(0.0);'),.62)
assert 155<=img.getpixel((160,90))[3]<=160,'alpha controls opacity'
assert img.getpixel((32,44))[3]==0,'rounded corner must be cut out'
assert img.getpixel((10,10))[3]==0,'outside panel must remain untouched'
assert max(render(frag,0).getchannel('A').getextrema())==0,'zero opacity'
def contrast(im):
 values=[im.getpixel((x,y))[0] for y in range(75,105) for x in range(100,220)]
 mean=sum(values)/len(values);return sum((v-mean)**2 for v in values)/len(values)
assert contrast(img)<contrast(reference)*.5,'local diffusion must reduce scene contrast'
output=root/'build/reports/glass/preview.png'
output.parent.mkdir(parents=True,exist_ok=True)
img.transpose(Image.Transpose.FLIP_TOP_BOTTOM).save(output)
print('GLSL 330 vertex + fragment shaders compiled and rendered on',c.info['GL_RENDERER'])
print('PASS: corner coverage, untouched outside area, 62% and 0% alpha, blur contrast reduction')
print('Preview:',output)
