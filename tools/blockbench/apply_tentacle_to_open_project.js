/* Run in Blockbench's developer console while the GeckoLib Entity project is open. */
(function () {
  if (typeof Project === 'undefined' || typeof Group === 'undefined' || typeof Cube === 'undefined') {
    throw new Error('Open the GeckoLib Entity project before running this script.');
  }
  if (Group.all.some(function (group) { return group.name === 'immersive_tentacle_root'; })) {
    Blockbench.showQuickMessage('ImmersiveBoss tentacle is already in this project.');
    return;
  }

  Project.texture_width = 64;
  Project.texture_height = 64;
  var root = new Group({name: 'immersive_tentacle_root', origin: [0, 0, 0], rotation: [0, 0, 0]}).init();
  var segments = [
    {name: 'tentacle_base', y: 0, h: 5, w: 8, rot: [0, 0, 0]},
    {name: 'tentacle_01', y: 5, h: 5, w: 7.2, rot: [0, 0, -7]},
    {name: 'tentacle_02', y: 10, h: 5, w: 6.6, rot: [0, 0, -10]},
    {name: 'tentacle_03', y: 15, h: 5, w: 6.0, rot: [0, 0, -14]},
    {name: 'tentacle_04', y: 20, h: 5, w: 5.4, rot: [0, 0, -18]},
    {name: 'tentacle_05', y: 25, h: 5, w: 4.8, rot: [0, 0, -22]},
    {name: 'tentacle_06', y: 30, h: 5, w: 4.2, rot: [0, 0, -25]},
    {name: 'tentacle_07', y: 35, h: 5, w: 3.6, rot: [0, 0, -28]},
    {name: 'tentacle_tip', y: 40, h: 5, w: 2.6, rot: [0, 0, -31]}
  ];
  var bones = [];
  segments.forEach(function (segment, index) {
    var parent = index ? bones[index - 1] : root;
    var bone = new Group({name: segment.name, origin: [0, segment.y, 0], rotation: segment.rot}).init().addTo(parent);
    bones.push(bone);
    var half = segment.w / 2;
    new Cube({name: segment.name + '_body', from: [-half, segment.y, -half], to: [half, segment.y + segment.h, half], box_uv: true, uv_offset: [0, 0], autouv: 0}).init().addTo(bone);
    if (index > 0 && index < 8) {
      var side = index % 2 ? 1 : -1;
      new Cube({name: segment.name + '_spike', from: [side * (half + 0.2), segment.y + 1.2, -0.8], to: [side * (half + 2.2), segment.y + 3.2, 0.8], box_uv: true, uv_offset: [32, 0], autouv: 0}).init().addTo(bone);
    }
  });

  var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect width="64" height="64" fill="#75a832"/><path fill="#9ed34b" d="M0 0h8v8H0zM16 8h8v8h-8zM32 0h8v8h-8zM48 16h8v8h-8zM8 32h8v8H8zM40 40h8v8h-8zM24 56h8v8h-8z"/><path fill="#4f822c" d="M8 16h8v8H8zM24 24h8v8h-8zM48 0h8v8h-8zM0 48h8v8H0zM40 56h8v8h-8z"/><path fill="#e35d83" d="M32 0h16v8H32zM48 8h16v8H48z"/></svg>';
  var texture = new Texture({name: 'immersive_tentacle', source: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)}).add(false);
  Cube.all.filter(function (cube) { return cube.name.indexOf('tentacle_') === 0; }).forEach(function (cube) { cube.applyTexture(texture); });

  if (typeof Animation !== 'undefined' && typeof Keyframe !== 'undefined') {
    var animation = new Animation({name: 'animation.immersive_tentacle.wave', loop: 'loop', length: 2}).add(false);
    bones.forEach(function (bone, index) {
      var animator = animation.getBoneAnimator(bone);
      if (!animator) return;
      var sway = (index % 2 ? 4 : -4) * (index / bones.length);
      new Keyframe({time: 0, channel: 'rotation', data_points: [{x: 0, y: 0, z: bone.rotation[2]}]}, null, animator).add();
      new Keyframe({time: 1, channel: 'rotation', data_points: [{x: 0, y: 0, z: bone.rotation[2] + sway}]}, null, animator).add();
      new Keyframe({time: 2, channel: 'rotation', data_points: [{x: 0, y: 0, z: bone.rotation[2]}]}, null, animator).add();
    });
  }
  Canvas.updateAll();
  if (typeof Undo !== 'undefined') Undo.finishEdit('Create ImmersiveBoss tentacle with texture and animation');
  Blockbench.showQuickMessage('触手、贴图和摆动动画已加入当前工程');
})();
